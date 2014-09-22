package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.banplugin.Util;
import de.static_interface.banplugin.model.Account;
import de.static_interface.banplugin.model.BanType;
import de.static_interface.sinklibrary.command.Command;
import de.static_interface.sinklibrary.sender.IrcCommandSender;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;

public class BanIpCommand extends Command {

    private MySQLDatabase db;

    public BanIpCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
    }

    @Override
    public boolean isIrcOpOnly() {
        return true;
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        String prefix = BukkitUtil.getSenderName(sender);
        String ip = args[0];

        if (!Util.isValidIp(ip)) {
            sender.sendMessage(ChatColor.DARK_RED + "\"" + ip + "\" ist kein gültige IP!");
            return true;
        }

        try {
            db.banIp(ip, sender.getName());
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
            return true;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            String playerIp = Util.getIp(p.getAddress().getAddress());
            if (ip.equals(playerIp)) {
                p.kickPlayer(ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Deine IP wurde gesperrt!");
            }
        }

        String bannedPlayers = null;

        try {
            List<Account> accounts = db.getAccounts(ip);
            for (Account acc : accounts) {
                db.ban(acc.getPlayername(), null, sender.getName(), acc.getUniqueId(), BanType.AUTO_IP);
                if (bannedPlayers == null) {
                    bannedPlayers = acc.getPlayername();
                    continue;
                }
                bannedPlayers += ", " + acc.getPlayername();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat die folgende IP gesperrt: " + ChatColor.RED + ip;
        BukkitUtil.broadcast(msg, "banplugin.notification", false);
        if (sender instanceof IrcCommandSender) {
            sender.sendMessage(msg);
        }
        if (bannedPlayers == null) {
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Folgende Spieler wurden automatisch gesperrt, da sie mit der selben IP spielten: ");
        sender.sendMessage(ChatColor.RED + bannedPlayers);
        return true;
    }
}
