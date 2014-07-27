package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.BanData;
import de.static_interface.banplugin.DateUtil;
import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.banplugin.Util;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.commands.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class IsBannedCommand extends Command
{
    private MySQLDatabase db;
    public IsBannedCommand(Plugin plugin, MySQLDatabase db)
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
        BanData data = null;

        if (args.length < 1)
        {
            return false;
        }

        String search = args[0];

        boolean ip = Util.isValidIp(search);

        String prefix = ip ? "Die IP " : "Spieler ";
        if(!ip)
        {
            search = SinkLibrary.getUser(search).getUniqueId().toString();
        }
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
