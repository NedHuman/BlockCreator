package dev.nedhuman.blockcreator.listener;

import dev.nedhuman.blockcreator.BlockCreator;
import dev.nedhuman.blockcreator.BlockCreatorService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class RemovalListeners implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.removeOwner(b.getLocation());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.removeOwner(b.getLocation());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.removeOwner(b.getLocation());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.removeOwner(b.getLocation());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.removeOwner(b.getLocation());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(StructureGrowEvent event) {
        for(BlockState i : event.getBlocks()) {
            Block b = i.getBlock();

            BlockCreatorService service = BlockCreator.getInstance().getService();
            service.removeOwner(b.getLocation());
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertalise(BlockFertilizeEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.removeOwner(b.getLocation());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDecay(LeavesDecayEvent event) {
        Block b = event.getBlock();

        BlockCreatorService service = BlockCreator.getInstance().getService();
        service.removeOwner(b.getLocation());
    }
}
