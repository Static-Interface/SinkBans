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

import de.static_interface.sinkbans.DateUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

@Description("Check if an account is banned")
@DefaultPermission
@Usage("<account>") //todo: add -i for ip searches
public class IsBannedCommand extends SinkCommand {
     public IsBannedCommand(Plugin plugin) {
         super(plugin);
         getCommandOptions().setMinRequiredArgs(1);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(args[0]);
        if (!user.isBanned()) {

            sender.sendMessage(ChatColor.GOLD + "Der Spieler " + ChatColor.RED + args[0] + ChatColor.GOLD + " ist nicht gebannt!");
            return true;
        } else {
            String
                    reason = user.getBanTimeOut() > 0 ?
                             ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(user.getBanTimeOut())
                                                      : user.getBanReason();
            reason = reason.trim();
            sender.sendMessage(ChatColor.GOLD + "Der Spieler " + ChatColor.RED + args[0] + ChatColor.GOLD +
                               " ist gebannt! Gebannt von: " + ChatColor.DARK_RED + user.getBannerDisplayName() + ChatColor.GOLD + ", Grund: "
                               + reason);

            return true;
        }
    }
}
