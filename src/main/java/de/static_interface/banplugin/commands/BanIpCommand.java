package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.banplugin.Util;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class BanIpCommand implements CommandExecutor
{
    private MySQLDatabase db;
    public BanIpCommand(MySQLDatabase db)
    {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (args.length < 1)
        {
            return false;
        }

        User user = SinkLibrary.getUser(sender);

        String prefix = user.isConsole() ? BukkitUtil.getSenderName(sender) : "Spieler " + user.getDisplayName();
        String ip = args[0];

        try
        {
            db.banIp(ip, user.getName());
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            user.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
            return true;
        }

        for(Player p : Bukkit.getOnlinePlayers())
        {
            String playerIp = Util.getIp(p.getAddress().getAddress());
            if ( ip.equals(playerIp) )
            {
                p.kickPlayer(ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Deine IP wurde gesperrt!");
            }
        }

        BukkitUtil.broadcastMessage(ChatColor.GOLD + prefix + ChatColor.GOLD + " hat die folgende IP gesperrt: "+ ChatColor.RED + ip, true);

        return true;
    }
}
