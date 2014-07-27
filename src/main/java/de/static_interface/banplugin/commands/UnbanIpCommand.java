package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.banplugin.Util;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.commands.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class UnbanIpCommand extends Command
{
    private MySQLDatabase db;
    public UnbanIpCommand(Plugin plugin, MySQLDatabase db)
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

        String ip = args[0];

        if(!Util.isValidIp(ip))
        {
            sender.sendMessage(ChatColor.DARK_RED +"\"" + ip + "\" ist kein gültige IP!");
            return true;
        }


        String prefix = BukkitUtil.getSenderName(sender);

        try
        {
            db.unbanIp(ip, sender.getName());
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            sender.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
            return true;
        }

        BukkitUtil.broadcastMessage(ChatColor.GOLD + prefix + ChatColor.GOLD + " hat die IP "+ ChatColor.RED + ip + ChatColor.GOLD + " entsperrt.", true);

        return true;
    }
}
