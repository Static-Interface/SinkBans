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

import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.sender.IrcCommandSender;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.Debug;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

@Description("Ban a player")
@DefaultPermission
@Usage("<player> [reason]")
public class BanCommand extends SinkCommand {
    public BanCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setIrcOpOnly(true);
        getCommandOptions().setMinRequiredArgs(1);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        String reason = "";
        for (int i = 1; i < args.length; i++) {
            reason += " " + args[i];
        }

        if (args.length == 1 || reason.isEmpty()) {
            reason = "Du wurdest permanent gebannt.";
        }

        reason = ChatColor.RED + reason;

        IngameUser target = SinkLibrary.getInstance().getIngameUser(args[0], false);

        Debug.log("Banning " + target.getName() + ", reason: " + reason);
        String targetName = target.isOnline() ? target.getName() : args[0];
        String prefix = BukkitUtil.getSenderName(sender);

        String reasonPrefixed = ChatColor.DARK_RED + "Gesperrt: ";

        if (target.isOnline()) {
            target.getPlayer().kickPlayer(reasonPrefixed + reason);
        }

        target.ban(SinkLibrary.getInstance().getUser(sender), reason);

        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat " + ChatColor.RED + targetName + ChatColor.GOLD + " gesperrt: " + reason.trim();
        SinkLibrary.getInstance().getMessageStream("sb_bans").sendMessage(null, msg, "sinkbans.notification");
        if (sender instanceof IrcCommandSender) {
            sender.sendMessage(msg);
        }

        return true;
    }
}
