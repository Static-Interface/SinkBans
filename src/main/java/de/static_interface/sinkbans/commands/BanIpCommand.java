/*
 * Copyright (c) 2013 - 2014 http://static-interface.de and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.sinkbans.commands;

import de.static_interface.sinkbans.MySQLDatabase;
import de.static_interface.sinkbans.Util;
import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinkbans.model.BanData;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.sender.IrcCommandSender;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;

public class BanIpCommand extends SinkCommand {

    private MySQLDatabase db;

    public BanIpCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        //todo: add regex
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

        for (Player p : BukkitUtil.getOnlinePlayers()) {
            String playerIp = Util.getIp(p.getAddress().getAddress());
            if (ip.equals(playerIp)) {
                p.kickPlayer(ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Deine IP wurde gesperrt!");
            }
        }

        String bannedPlayers = null;

        try {
            List<Account> accounts = db.getAccounts(ip);
            for (Account acc : accounts) {
                boolean cancelBan = false;
                for(BanData data : db.getBanData(acc.getUniqueId().toString(), false)) {
                    if(data.isBanned()) {
                        cancelBan = true;
                        break;
                    }
                }
                if(cancelBan) {
                    continue;
                }
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
        BukkitUtil.broadcast(msg, "sinkbans.notification", false);
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
