package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.BanData;
import de.static_interface.banplugin.DateUtil;
import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.banplugin.Util;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.commands.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        List<BanData> datas = new ArrayList<>();

        if (args.length < 1)
        {
            return false;
        }

        String search = args[0];

        boolean ip = Util.isValidIp(search);

        String prefix = ip ? "Die IP " : "Spieler ";
        if(!ip)
        {
            try
            {
                datas = db.getOldBanData(search);
                search = BukkitUtil.getUUIDByName(search).toString();
                datas.addAll(db.getBanData(search, false));
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
        try
        {
            datas = db.getBanData(search, ip);
        }
        catch ( SQLException ignored ) { }

        for(BanData data : datas)
        {
            if ( data.isBanned() )
            {
                String reason = data.isTempBanned() ? ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(data.getUnbanTimeStamp()) : data.getReason();
                reason = reason.trim();
                sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + args[0] + ChatColor.GOLD +
                        " ist gebannt! Gebannt von: " + ChatColor.DARK_RED + data.getBanner() + ChatColor.GOLD + ", Grund: " + reason);
                return true;
            }
        }

        sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + args[0] + ChatColor.GOLD + " ist nicht gebannt!");
        return true;
    }
}
