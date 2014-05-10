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

public class BanCommand implements CommandExecutor
{
    private MySQLDatabase db;
    public BanCommand(MySQLDatabase db)
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

        String reason = "";
        for(int i = 1; i < args.length; i++)
        {
            reason += " " + args[i];
        }

        if (args.length == 1 || reason.isEmpty())
        {
            reason = "Du wurdest permanent gebannt.";
        }

        reason = ChatColor.RED + reason;

        User user = SinkLibrary.getUser(sender);
        User target = SinkLibrary.getUser(args[0]);

        String targetName = target.isOnline() ? target.getName() : args[0];
        String prefix = user.isConsole() ? BukkitUtil.getSenderName(sender) : "Spieler " + user.getDisplayName();

        String reasonPrefix = ChatColor.DARK_RED + "Gesperrt: ";

        if (target.isOnline())
        {
            target.getPlayer().kickPlayer(reasonPrefix + reason);
        }

        try
        {
            db.ban(target.getName(), reason, user.getName());
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            user.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
            return true;
        }

        BukkitUtil.broadcastMessage(ChatColor.GOLD + prefix + ChatColor.GOLD + " hat " + ChatColor.RED + targetName + ChatColor.GOLD  + " gesperrt:" + reason.trim(), true);

        return true;
    }
}
