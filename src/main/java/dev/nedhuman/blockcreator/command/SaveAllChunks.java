package dev.nedhuman.blockcreator.command;

import dev.nedhuman.blockcreator.BlockCreator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SaveAllChunks implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player player)) {
            return true;
        }
        if(!player.hasPermission("blockcreator.save")) {
            return true;
        }

        BlockCreator.getInstance().getService().saveChunks();
        player.sendMessage(ChatColor.YELLOW+"Saved all chunks");
        return true;
    }
}
