/*
 * Copyright (c) 2013 - 2015 http://static-interface.de and contributors
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

import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.exception.UserNotOnlineException;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.StringUtil;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

@Description("Kick a user")
@Usage("<player> [reason]")
@DefaultPermission
public class KickCommand extends SinkCommand {
    public KickCommand(@Nonnull Plugin plugin) {
        super(plugin);
        getCommandOptions().setMinRequiredArgs(1);
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        IngameUser user = getArg(args, 0, IngameUser.class);
        String reason = "Von Server geworfen...";
        if(args.length >= 2) {
            reason = StringUtil.formatArrayToString(args, " ", 1);
        }

        if(!user.isOnline()) {
            throw new UserNotOnlineException(user.getName());
        }

        if(user.isOp() && !sender.isOp()) {
            user.sendMessage("Cannot kick this person");
            return true;
        }

        user.getPlayer().kickPlayer(ChatColor.DARK_RED + "Kicked: " + ChatColor.RED + reason);
        String msg = SinkLibrary.getInstance().getUser(sender).getDisplayName() + ChatColor.RESET + ChatColor.RED
                     + " hat rausgeworfen: " + user.getDisplayName() + ChatColor.RESET + ChatColor.RED + " für: "  + ChatColor.RESET
                     + reason;
        SinkLibrary.getInstance().getMessageStream("sb_bans").sendMessage(null, msg, getSubPermission("notification"));
        return false;
    }
}
