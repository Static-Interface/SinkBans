package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

public class UnbanIpCommand implements CommandExecutor
{
    private MySQLDatabase db;
    public UnbanIpCommand(MySQLDatabase db)
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

        String ip = args[0];
        String prefix = user.isConsole() ? BukkitUtil.getSenderName(sender) : "Spieler " + user.getDisplayName();

        try
        {
            db.unbanIp(ip, user.getName());
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            user.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
            return true;
        }

        BukkitUtil.broadcastMessage(ChatColor.GOLD + prefix + ChatColor.GOLD + " hat die IP "+ ChatColor.RED + ip + ChatColor.GOLD + " entsperrt.", true);

        return true;
    }
}
