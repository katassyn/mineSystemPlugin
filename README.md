# MineSystemPlugin

This plugin adds custom mining mechanics such as timed spheres and special ores.

## Sphere schematics

Schematics are loaded from `plugins/MineSystemPlugin/schematics/<Type>` where
`<Type>` is one of:

- `Ore`
- `Treasure`
- `Vegetation`
- `Mob`
- `Boss`
- `SpecialEvent`
- `Puzzle`
- `CrystalDust`

Files can have any name as long as they end with the `.schem` extension. The
plugin randomly selects the sphere type based on configured weights and then
chooses a random schematic from the corresponding folder.

## Registering Event Listeners

Event listeners are registered through the Bukkit `PluginManager`. The main plugin
class exposes a helper method for convenience:

```java
private void registerListener(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
}
```

Call this method from `onEnable` to hook your listener:

```java
@Override
public void onEnable() {
    registerListener(new MyCustomListener());
}
```

Your listener can then react to custom events like `OreMinedEvent` and
`SphereCompleteEvent`.
