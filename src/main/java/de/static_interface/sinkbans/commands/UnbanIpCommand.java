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

import de.static_interface.sinkbans.Util;
import de.static_interface.sinkbans.database.DatabaseBanManager;
import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.sender.IrcCommandSender;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;
@Description("Unban an ip with all accounts")
@DefaultPermission
@Usage("<ip>")
public class UnbanIpCommand extends SinkCommand {
    public UnbanIpCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setIrcOpOnly(true);
        getCommandOptions().setMinRequiredArgs(1);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        String ip = args[0];

        if (!Util.isValidIp(ip)) {
            sender.sendMessage(ChatColor.DARK_RED + "\"" + ip + "\" ist kein gültige IP!");
            return true;
        }

        String prefix = BukkitUtil.getSenderName(sender);

        DatabaseBanManager.unbanIp(ip, sender.getName());

        String unbannedPlayers = null;
        List<Account> accounts  = DatabaseBanManager.getAccounts(ip);
        for (Account acc : accounts) {
            DatabaseBanManager.unban(acc.getUniqueId(), sender.getName());
            if (unbannedPlayers == null) {
                unbannedPlayers = acc.getPlayername();
                continue;
            }
            unbannedPlayers += ", " + acc.getPlayername();
        }

        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat die IP " + ChatColor.RED + ip + ChatColor.GOLD + " entsperrt.";
        SinkLibrary.getInstance().getMessageStream("sb_bans_ip").sendMessage(null, msg, "sinkbans.notification");
        if (sender instanceof IrcCommandSender) {
            sender.sendMessage(msg);
        }

        if (unbannedPlayers == null) {
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Folgende Spieler wurden automatisch entsperrt, da sie mit der selben IP spielten: ");
        sender.sendMessage(ChatColor.DARK_GREEN + unbannedPlayers);
        return true;
    }
}
