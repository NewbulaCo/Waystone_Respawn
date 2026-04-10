package com.newbulaco.waystonerespawn;

import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.TeleportDestination;
import net.blay09.mods.waystones.api.WaystoneActivatedEvent;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Optional;
import java.util.UUID;

public class SpawnHandler {

    private static final Logger log = LogUtils.getLogger();

    // nbt keys stored under Player.PERSISTED_NBT_TAG so they survive death
    private static final String TAG_ROOT = "WaystoneRespawn";
    private static final String TAG_UUID_MOST = "UUIDMost";
    private static final String TAG_UUID_LEAST = "UUIDLeast";

    // --- forge events ---

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        // block all vanilla spawn point setting (beds, respawn anchors)
        // waystones are the only way to set your respawn now
        event.setCanceled(true);

        // only notify when something was actually trying to set a spawn (not on clears)
        if (event.getNewSpawn() != null && event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(
                    Component.translatable("waystonerespawn.bed_disabled").withStyle(ChatFormatting.GRAY),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID waystoneId = loadWaystoneId(player);
        if (waystoneId == null) return;

        Optional<IWaystone> waystone = WaystonesAPI.getWaystone(player.server, waystoneId);
        if (waystone.isEmpty() || !waystone.get().isValid()) {
            // waystone was destroyed - clear the stored data, player stays at world spawn
            clearWaystoneSpawn(player);
            player.displayClientMessage(
                    Component.translatable("waystonerespawn.spawn_lost").withStyle(ChatFormatting.RED),
                    false
            );
            return;
        }

        IWaystone ws = waystone.get();
        ServerLevel targetLevel = player.server.getLevel(ws.getDimension());
        if (targetLevel == null) return;

        // ask waystones for the safe drop-off spot it would use for a normal teleport
        // (this lands you next to the waystone facing it, not on top of the block)
        TeleportDestination dest = ws.resolveDestination(targetLevel);
        Vec3 loc = dest.getLocation();
        float yaw = dest.getDirection().toYRot();

        if (targetLevel == player.serverLevel()) {
            player.teleportTo(targetLevel, loc.x, loc.y, loc.z, yaw, 0f);
        } else {
            player.teleportTo(targetLevel, loc.x, loc.y, loc.z, yaw, 0f);
        }
        player.setYHeadRot(yaw);

        log.debug("respawned {} at waystone '{}' ({}, {}, {} in {})",
                player.getName().getString(), ws.getName(),
                loc.x, loc.y, loc.z, ws.getDimension().location());
    }

    // --- balm events (registered in WaystoneRespawn.setup) ---

    public static void onWaystoneActivated(WaystoneActivatedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            saveWaystoneSpawn(player, event.getWaystone());
        }
    }

    public static void onWaystoneTeleport(WaystoneTeleportEvent.Post event) {
        if (event.getContext().getEntity() instanceof ServerPlayer player) {
            saveWaystoneSpawn(player, event.getContext().getTargetWaystone());
        }
    }

    // --- persistent nbt storage ---

    private static void saveWaystoneSpawn(ServerPlayer player, IWaystone waystone) {
        CompoundTag persisted = getOrCreatePersisted(player);
        CompoundTag tag = new CompoundTag();
        UUID uid = waystone.getWaystoneUid();
        tag.putLong(TAG_UUID_MOST, uid.getMostSignificantBits());
        tag.putLong(TAG_UUID_LEAST, uid.getLeastSignificantBits());
        persisted.put(TAG_ROOT, tag);

        player.displayClientMessage(
                Component.translatable("waystonerespawn.spawn_set").withStyle(ChatFormatting.GRAY),
                true
        );

        log.debug("set respawn waystone for {} to '{}' ({})",
                player.getName().getString(), waystone.getName(), uid);
    }

    private static UUID loadWaystoneId(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(TAG_ROOT)) return null;

        CompoundTag tag = persisted.getCompound(TAG_ROOT);
        if (!tag.contains(TAG_UUID_MOST)) return null;

        return new UUID(tag.getLong(TAG_UUID_MOST), tag.getLong(TAG_UUID_LEAST));
    }

    private static void clearWaystoneSpawn(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(TAG_ROOT);
    }

    private static CompoundTag getOrCreatePersisted(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
