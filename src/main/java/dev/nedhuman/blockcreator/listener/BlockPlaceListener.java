package dev.nedhuman.blockcreator.listener;

import dev.nedhuman.blockcreator.BlockCreator;
import dev.nedhuman.blockcreator.BlockCreatorService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.setOwner(b.getLocation(), event.getPlayer().getUniqueId());
    }
}
