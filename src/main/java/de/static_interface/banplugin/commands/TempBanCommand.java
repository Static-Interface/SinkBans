package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.DateUtil;
import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.SinkUser;
import de.static_interface.sinklibrary.commands.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class TempBanCommand extends Command
{
    private MySQLDatabase db;
    public TempBanCommand(Plugin plugin, MySQLDatabase db)
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

        SinkUser target = SinkLibrary.getUser(args[0]);

        String targetName = target.isOnline() ? target.getName() : args[0];
        String prefix = BukkitUtil.getSenderName(sender);

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
            db.tempBan(target.getName(), banTimestamp, sender.getName(), target.getUniqueId());
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
