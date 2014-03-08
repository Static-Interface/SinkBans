package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.BanData;
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
        BanData data;

        if (args.length < 1)
        {
            return false;
        }

        String name = args[0];
        try
        {
            data = db.getBanData(name);
        }
        catch ( SQLException e )
        {
            throw new RuntimeException(e);
        }

        if (data != null && (data.isBanned() || data.isTempBanned()) )
        {
            String reason = data.getReason();
            sender.sendMessage(ChatColor.GOLD + "Spieler " + ChatColor.RED + name + ChatColor.GOLD +
                    " wurde von " + ChatColor.DARK_RED + data.getBanner() + ChatColor.GOLD + " gebannt: " + reason);
        }

        sender.sendMessage(ChatColor.GOLD + "Spieler " + ChatColor.RED + name + ChatColor.GOLD + " ist nicht gebannt!");

        return true;
    }
}
