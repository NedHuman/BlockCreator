package dev.nedhuman.blockcreator;

import org.bukkit.plugin.java.JavaPlugin;

public final class BlockCreator extends JavaPlugin {

    private static BlockCreator instance;

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static BlockCreator getInstance() {
        return instance;
    }
}
