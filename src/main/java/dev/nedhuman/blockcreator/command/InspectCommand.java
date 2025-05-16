package dev.nedhuman.blockcreator.command;

import dev.nedhuman.blockcreator.BlockCreator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class InspectCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player player)) {
            return true;
        }
        if(!player.hasPermission("blockcreator.inspect")) {
            return true;
        }

        Set<UUID> inspectors = BlockCreator.getInstance().getInspecting();
        boolean inspecting = inspectors.contains(player.getUniqueId());

        if(inspecting) {
            player.sendMessage(ChatColor.YELLOW+"Inspector Disabled");
            inspectors.remove(player.getUniqueId());
        }else {
            player.sendMessage(ChatColor.YELLOW+"Inspector Enabled");
            inspectors.add(player.getUniqueId());
        }

        return true;
    }
}
