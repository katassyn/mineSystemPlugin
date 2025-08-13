# MineSystemPlugin

MineSystemPlugin adds an instanced mining experience to Paper/Spigot servers.
Players enter temporary spheres built from schematics and gather custom ores,
fight mobs or bosses, and earn configurable loot and currency.

## Features
- Weighted random sphere generation using WorldEdit schematics
- Custom ores with durability and random variants
- Timed spheres that clear themselves and remove spawned mobs
- Loot editor GUI and per-schematic special loot with Save/Cancel controls
- Special loot chests always roll items per slot and contain at least three rewards
- Optional Citizens NPCs for special1.schem and special2.schem
- Mob spawning that requires a solid ceiling to keep entities inside
- Spheres spawn only within -753,-61,-1281 to -381,143,-1658 and never overlap, keeping the world size in check
- Entering a sphere shows its type in a title
- Stamina system with quest-based max stamina bonuses
- Pickaxe repair/enchant commands using Crystal currency
- Database-backed persistence for players, pickaxes, loot and schematics

## Requirements
- Java 17+
- Paper/Spigot 1.20.1 server
- [Vault](https://www.spigotmc.org/resources/vault.34315/) for economy support
- [FastAsyncWorldEdit](https://www.spigotmc.org/resources/fast-async-worldedit-fawe.13932/) or WorldEdit for schematics
- [MythicMobs](https://www.mythicmobs.net/) for custom mob spawns
- [Citizens](https://www.spigotmc.org/resources/citizens.13811/) if using special sphere NPCs
- MySQL database (via HikariCP)

## Building
1. Clone the repository
2. Run `mvn package`
3. Copy `target/minesystemplugin-1.0-SNAPSHOT.jar` to your server's `plugins` folder

## Configuration
Configuration is stored in `plugins/MineSystemPlugin/config.yml`.

### Database
Provide MySQL connection details under the `database` section:

```yaml
database:
  host: localhost
  port: 3306
  name: minesystem
  user: root
  password: secret
```

### Mob spawns
Define custom mobs tied to schematics:

```yaml
mobs:
  example.schem:
    - name: ExampleMob
      mythic_id: example_id
      amount: 3
```

Each entry is spawned when the schematic is pasted.

### Sell prices
`config.yml` maps custom ore IDs to sell prices per world:

```yaml
sell-prices:
  world:
    Hematite: 1000000
    BlackSpinel: 1750000
    ...
```

### Debug flags
Toggle various debug logs:

```yaml
debug:
  toolListener: false
```

## Sphere schematics
Schematics live under `plugins/MineSystemPlugin/schematics/<Type>` where `<Type>`
is one of:

- `Ore`
- `Treasure`
- `Vegetation`
- `Mob`
- `Boss`
- `SpecialEvent`
- `CrystalDust`

Files must use the `.schem` extension. The plugin chooses a sphere type based on
built‑in weights and then selects a random schematic from that folder. If a type
has no schematics, it is removed from the pool and remaining weights are
renormalized so the total chance stays 100 %.

## Special spheres
When `special1.schem` or `special2.schem` is pasted, the plugin clones a
Citizens NPC onto the diamond block at the sphere's center:

| Schematic       | Template NPC ID |
| --------------- | --------------- |
| `special1.schem`| 61              |
| `special2.schem`| 62              |

The copied NPC is removed again when the sphere expires.

## Loot editing
Use `/loot` to edit global sphere loot and `/specialloot <schematic>` for
schematic‑specific rewards. The schematic name may be given with or without
the `.schem` extension. `/specialloot test <schematic>` previews the random
loot for a schematic. Both edit commands open a 54‑slot inventory:

- Items dragged from the player's inventory into the GUI are added with a
  default 50 % chance.
- Change a chance by clicking an item.
- Use the green **Save** wool to persist changes to the database or red **Cancel**
  to discard.
- Special loot menus also save automatically when closed.
- If the combined chances go over 100 %, they are automatically scaled so their
  proportions remain the same while the total equals 100 %.
- Special loot generation fills at least three slots, picking an item for each from
  the weighted chances so chests are never empty.

## Stamina system
Players consume stamina when entering spheres. Stamina regenerates after a delay
and can be increased through quests. `/stamin` shows the current amount and time
until reset.

## Pickaxes and crystals
Mining uses custom pickaxes tracked in the database. `/mine_repair` restores
durability using crystals and `/mine_enchant` applies crystal‑based enchants.
Random bonus items may drop when a sphere is completed.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/loot` | Manage loot configuration | `minesystemplugin.loot` |
| `/mine` | Open the mine selection menu | `minesystemplugin.mine` |
| `/mine_repair` | Repair the item in hand using crystals | `minesystemplugin.mine` |
| `/mine_enchant` | Enchant a pickaxe using crystals | `minesystemplugin.mine` |
| `/spawnsphere` | Spawn a sphere for testing | `minesystemplugin.mine` |
| `/specialloot <schematic>` | Edit loot for a specific schematic (extension optional) | `minesystemplugin.mine` |
| `/specialloot test <schematic>` | Preview generated special loot (extension optional) | `minesystemplugin.mine` |
| `/stamin` | Check your stamina | `minesystemplugin.mine` |

The `minesystem.admin` permission bypasses mining restrictions.

## API events
Other plugins can listen for:

- `OreMinedEvent` – fired when a custom ore is broken
- `SphereCompleteEvent` – fired when a sphere finishes

Register listeners through Bukkit's `PluginManager`:

```java
private void registerListener(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
}
```

Call this method from `onEnable` to hook your listener.

## License
*(Add license information here if applicable.)*

