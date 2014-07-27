package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.banplugin.Util;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.commands.Command;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class BanIpCommand extends Command
{
    private MySQLDatabase db;
    public BanIpCommand(Plugin plugin, MySQLDatabase db)
    {
        super(plugin);
        this.db = db;
    }

    @Override
    public boolean isIrcOpOnly()
    {
        return true;
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args)
    {
        if (args.length < 1)
        {
            return false;
        }

        String prefix = BukkitUtil.getSenderName(sender);
        String ip = args[0];

        if(!Util.isValidIp(ip))
        {
            sender.sendMessage(ChatColor.DARK_RED +"\"" + ip + "\" ist kein gültige IP!");
            return true;
        }

        try
        {
            db.banIp(ip, sender.getName());
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            sender.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
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
