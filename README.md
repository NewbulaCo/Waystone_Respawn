# Waystone Respawn

A Minecraft Forge mod for 1.20.1 that replaces bed respawning with waystone respawning.

Beds and respawn anchors no longer set your spawn point. Instead, the last waystone you used (activated or teleported to) becomes your respawn point. If that waystone is destroyed, you fall back to world spawn.

The goal is to make survival a little harder by removing the cheap, anywhere-you-can-place-a-bed safety net.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- [Waystones](https://www.curseforge.com/minecraft/mc-mods/waystones) (14.x)
- [Balm](https://www.curseforge.com/minecraft/mc-mods/balm) (7.x)

## Installation

Drop the jar into your `mods/` folder alongside Waystones and Balm.

This mod is server-side only — clients connecting to a server with it installed do not need it themselves.

## Building

```
./gradlew build
```

The jar will appear in `build/libs/`.
