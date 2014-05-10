package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.BanData;
import de.static_interface.banplugin.DateUtil;
import de.static_interface.banplugin.MySQLDatabase;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.regex.Pattern;

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

        String pattern = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";

        boolean ip = Pattern.matches(pattern, search);

        String prefix = ip ? "Die IP " : "Spieler ";

        try
        {
            data = db.getBanData(search, ip);
        }
        catch ( SQLException ignored ) { }

        if (data != null && data.isBanned())
        {
            String reason = data.isTempBanned() ? ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(data.getUnbanTimestamp())
                    : data.getReason();
            reason = reason.trim();
            sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + search + ChatColor.GOLD +
                    " ist gebannt! Gebannt von: " + ChatColor.DARK_RED + data.getBanner() + ChatColor.GOLD + ", Grund: " + reason);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + search + ChatColor.GOLD + " ist nicht gebannt!");

        return true;
    }
}
