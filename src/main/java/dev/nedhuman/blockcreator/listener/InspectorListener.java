package dev.nedhuman.blockcreator.listener;

import dev.nedhuman.blockcreator.BlockCreator;
import dev.nedhuman.blockcreator.BlockCreatorService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class InspectorListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if((event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
                BlockCreator.getInstance().getInspecting().contains(player.getUniqueId())) {
            event.setCancelled(true);

            Location location = event.getClickedBlock().getLocation();

            BlockCreatorService service = BlockCreator.getInstance().getService();
            if(service.hasOwner(location)) {
                UUID owner = service.getOwner(location);
                player.sendMessage(ChatColor.YELLOW+"Block owner is "+ChatColor.AQUA+owner.toString());
                player.sendMessage(ChatColor.YELLOW+"Username "+ChatColor.RED+ Bukkit.getOfflinePlayer(owner).getName());
            }else{
                player.sendMessage(ChatColor.YELLOW+"Block has no owner");
            }
        }
    }
}
