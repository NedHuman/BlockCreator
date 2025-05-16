package dev.nedhuman.blockcreator.command;

import dev.nedhuman.blockcreator.BlockCreator;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class DebugDumpDataCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player player)) {
            return true;
        }
        if(!player.hasPermission("blockcreator.debug")) {
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if(!pdc.has(BlockCreator.getInstance().getService().getDataKey()) || !pdc.has(BlockCreator.getInstance().getService().getUsersKey())) {
            player.sendMessage(ChatColor.YELLOW+"Chunk does not have data");
            return true;
        }

        StringBuilder users = new StringBuilder();
        byte[] usersArr = pdc.get(BlockCreator.getInstance().getService().getUsersKey(), PersistentDataType.BYTE_ARRAY);
        for(byte i : usersArr) {
            users.append("0x").append(Integer.toHexString(i & 0xFF)).append(" ");
        }

        StringBuilder data = new StringBuilder();
        byte[] dataArr = pdc.get(BlockCreator.getInstance().getService().getDataKey(), PersistentDataType.BYTE_ARRAY);
        for(byte i : dataArr) {
            data.append("0x").append(Integer.toHexString(i & 0xFF)).append(" ");
        }

        player.sendMessage(ChatColor.YELLOW+"Info for chunk at "+chunk.getX()+" "+chunk.getZ());
        player.sendMessage(ChatColor.YELLOW+"Users "+ChatColor.AQUA+users);
        player.sendMessage(ChatColor.YELLOW+"Data "+ChatColor.AQUA+data);
        return true;
    }
}
