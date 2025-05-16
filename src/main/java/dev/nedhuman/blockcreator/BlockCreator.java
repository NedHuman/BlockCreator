package dev.nedhuman.blockcreator;

import dev.nedhuman.blockcreator.command.DebugDumpDataCommand;
import dev.nedhuman.blockcreator.command.InspectCommand;
import dev.nedhuman.blockcreator.command.SaveAllChunks;
import dev.nedhuman.blockcreator.listener.BlockPlaceListener;
import dev.nedhuman.blockcreator.listener.InspectorListener;
import dev.nedhuman.blockcreator.listener.RemovalListeners;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BlockCreator extends JavaPlugin {

    private static BlockCreator instance;

    private BlockCreatorService service;

    private Set<UUID> inspecting;
    private boolean debug;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        debug = getConfig().getBoolean("debug");

        getServer().getPluginManager().registerEvents(new BlockPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new RemovalListeners(), this);
        getServer().getPluginManager().registerEvents(new InspectorListener(), this);
        getCommand("bcinspect").setExecutor(new InspectCommand());
        getCommand("bcdebug").setExecutor(new DebugDumpDataCommand());
        getCommand("bcsave").setExecutor(new SaveAllChunks());
        service = new BlockCreatorService(this);

        inspecting = new HashSet<>();
    }

    public Set<UUID> getInspecting() {
        return inspecting;
    }
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void onDisable() {
        service.saveChunks();
    }

    public BlockCreatorService getService() {
        return service;
    }

    public static BlockCreator getInstance() {
        return instance;
    }
}
