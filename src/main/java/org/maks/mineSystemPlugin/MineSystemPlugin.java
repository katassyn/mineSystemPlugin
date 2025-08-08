package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.database.DatabaseManager;
import org.maks.mineSystemPlugin.database.dao.PickaxesDao;
import org.maks.mineSystemPlugin.database.dao.PlayersDao;
import org.maks.mineSystemPlugin.database.dao.QuestsDao;
import org.maks.mineSystemPlugin.database.dao.SpheresDao;

public final class MineSystemPlugin extends JavaPlugin {
    private DatabaseManager database;
    private PlayersDao playersDao;
    private PickaxesDao pickaxesDao;
    private QuestsDao questsDao;
    private SpheresDao spheresDao;

    @Override
    public void onEnable() {
        database = new DatabaseManager(this);
        playersDao = new PlayersDao(database);
        pickaxesDao = new PickaxesDao(database);
        questsDao = new QuestsDao(database);
        spheresDao = new SpheresDao(database);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    public PlayersDao getPlayersDao() {
        return playersDao;
    }

    public PickaxesDao getPickaxesDao() {
        return pickaxesDao;
    }

    public QuestsDao getQuestsDao() {
        return questsDao;
    }

    public SpheresDao getSpheresDao() {
        return spheresDao;
    }
}

