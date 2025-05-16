package dev.nedhuman.blockcreator;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

public class BlockCreatorServiceListeners implements Listener {

    private final BlockCreatorService service;

    protected BlockCreatorServiceListeners(BlockCreatorService service, Plugin plugin) {
        this.service = service;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        service.fireChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.fireChunkUnload(event.getChunk(), false);
    }
}
