package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.DateUtil;
import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

public class TempBanCommand implements CommandExecutor
{
    private MySQLDatabase db;
    public TempBanCommand(MySQLDatabase db)
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
        User target = SinkLibrary.getUser(args[0]);

        String targetName = target.isOnline() ? target.getName() : args[0];
        String prefix = user.isConsole() ? BukkitUtil.getSenderName(sender) : "Spieler " + user.getDisplayName();

        final String time = getFinalArg(args, 1);
        final long banTimestamp;
        try
        {
            banTimestamp = DateUtil.parseDateDiff(time, true);
        }
        catch ( Exception e )
        {
            sender.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + time + " ist keine gueltige Zeitangabe!");
            return true;
        }


        String reason = ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(banTimestamp);
        String reasonPrefix = ChatColor.DARK_RED + "Gesperrt: ";

        if(target.isOnline())
        {
            target.getPlayer().kickPlayer(reasonPrefix + reason);
        }

        try
        {
            db.tempBan(target.getName(), banTimestamp, user.getName());
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        BukkitUtil.broadcastMessage(ChatColor.GOLD + prefix + ChatColor.GOLD + " hat " + ChatColor.RED + targetName + ChatColor.GOLD + " gesperrt: " + reason.trim(), true);

        return true;
    }

    public static String getFinalArg(final String[] args, final int start)
    {
        final StringBuilder bldr = new StringBuilder();
        for (int i = start; i < args.length; i++)
        {
            if (i != start)
            {
                bldr.append(" ");
            }
            bldr.append(args[i]);
        }
        return bldr.toString();
    }
}
