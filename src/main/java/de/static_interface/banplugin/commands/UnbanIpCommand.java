package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.Account;
import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.banplugin.Util;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.commands.Command;
import de.static_interface.sinklibrary.irc.IrcCommandSender;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;

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

        String unbannedPlayers = null;
        try
        {
            List<Account> accounts = db.getAccounts(ip);
            for(Account acc : accounts)
            {
                db.unban(acc.getUniqueId(), acc.getPlayername(), sender.getName());
                if(unbannedPlayers == null)
                {
                    unbannedPlayers = acc.getPlayername();
                    continue;
                }
                unbannedPlayers += ", " + acc.getPlayername();
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat die IP "+ ChatColor.RED + ip + ChatColor.GOLD + " entsperrt.";
        BukkitUtil.broadcast(msg, "banplugin.notification", false);
        if(sender instanceof IrcCommandSender )
        {
            sender.sendMessage(msg);
        }

        if(unbannedPlayers == null) return true;
        sender.sendMessage(ChatColor.GOLD + "Folgende Spieler wurden automatisch entsperrt, da sie mit der selben IP spielten: ");
        sender.sendMessage(ChatColor.DARK_GREEN + unbannedPlayers);
        return true;
    }
}
