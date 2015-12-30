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

import de.static_interface.sinkbans.database.DatabaseBanManager;
import de.static_interface.sinkbans.Util;
import de.static_interface.sinkbans.database.BanRow;
import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.sender.IrcCommandSender;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@Description("Ban an ip with all accounts")
@DefaultPermission
@Usage("<ip> [reason]")
public class BanIpCommand extends SinkCommand {
    public BanIpCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setIrcOpOnly(true);
        getCommandOptions().setMinRequiredArgs(1);
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

        DatabaseBanManager.banIp(ip, sender.getName());

        for (Player p : Bukkit.getOnlinePlayers()) {
            String playerIp = Util.getIp(p.getAddress().getAddress());
            if (ip.equals(playerIp)) {
                p.kickPlayer(ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Deine IP wurde gesperrt!");
            }
        }

        String bannedPlayers = null;

        List<Account> accounts = DatabaseBanManager.getAccounts(ip);
        for (Account acc : accounts) {
            IngameUser user = SinkLibrary.getInstance().getIngameUser(acc.getUniqueId());

            boolean cancelBan = false;
            for(BanRow row : DatabaseBanManager.getUserBans(user)) {
                if(row.isbanned) {
                    cancelBan = true;
                    break;
                }
            }
            if(cancelBan) {
                continue;
            }

            user.ban(SinkLibrary.getInstance().getConsoleUser(), "Automatischer Bann durch IP Bann");
            if (bannedPlayers == null) {
                bannedPlayers = acc.getPlayername();
                continue;
            }
            bannedPlayers += ", " + acc.getPlayername();
        }

        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat die folgende IP gesperrt: " + ChatColor.RED + ip;
        SinkLibrary.getInstance().getMessageStream("sb_bans_ip").sendMessage(null, msg, "sinkbans.notification");
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
