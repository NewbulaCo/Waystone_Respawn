package com.newbulaco.waystonerespawn;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.WaystoneActivatedEvent;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(WaystoneRespawn.MOD_ID)
public class WaystoneRespawn {

    public static final String MOD_ID = "waystonerespawn";
    private static final Logger log = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public WaystoneRespawn() {
        MinecraftForge.EVENT_BUS.register(SpawnHandler.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        // register for waystones events through balm's event bus
        Balm.getEvents().onEvent(WaystoneActivatedEvent.class, SpawnHandler::onWaystoneActivated);
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Post.class, SpawnHandler::onWaystoneTeleport);
        log.info("waystone respawn loaded - beds and respawn anchors disabled, waystones are your new spawn points");
    }
}
