# MineSystemPlugin

This plugin adds custom mining mechanics such as timed spheres and special ores.

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
