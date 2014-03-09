package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.BanData;
import de.static_interface.banplugin.DateUtil;
import de.static_interface.banplugin.MySQLDatabase;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

public class IsBannedCommand implements CommandExecutor
{
    private MySQLDatabase db;
    public IsBannedCommand(MySQLDatabase db)
    {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        BanData data = null;

        if (args.length < 1)
        {
            return false;
        }

        String search = args[0];
        boolean ip = false;

        if( args[0].equals("ip") )
        {
            if(args.length < 2)
            {
                return false;
            }
            search = args[1];
            ip = true;
        }

        String prefix = ip ? "Die IP " : "Spieler ";

        try
        {
            data = db.getBanData(search, ip);
        }
        catch ( SQLException ignored ) { }

        String reason = ip ? ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(data.getUnbanTimestamp())
                    : data.getReason();

        if (data != null && (data.isBanned() || data.isTempBanned()) )
        {
            sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + search + ChatColor.GOLD +
                    " wurde von " + ChatColor.DARK_RED + data.getBanner() + ChatColor.GOLD + " gebannt: " + reason);
        }

        sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + search + ChatColor.GOLD + " ist nicht gebannt!");

        return true;
    }
}
