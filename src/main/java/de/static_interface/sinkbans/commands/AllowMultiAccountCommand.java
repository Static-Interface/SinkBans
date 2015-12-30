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
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
@Description("Add a player to the multiaccount whitelist")
@DefaultPermission
@Usage("<player>")
public class AllowMultiAccountCommand extends SinkCommand {
    public AllowMultiAccountCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setIrcOpOnly(true);
        getCommandOptions().setMinRequiredArgs(1);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(args[0], false);

        if (DatabaseBanManager.isAllowedMultiAccount(user)) {
            sender.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + " Dieser Account ist bereits auf der Whitelist!");
            return true;
        }
        DatabaseBanManager.addMultiAccount(user);
        sender.sendMessage(ChatColor.DARK_GREEN + user.getName()  + " wurde erfolgreich zur MultiAccount Whitelist hinzugefügt!");
        return true;
    }
}
