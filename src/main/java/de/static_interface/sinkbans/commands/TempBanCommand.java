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
import de.static_interface.sinkbans.MySQLDatabase;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.sender.IrcCommandSender;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class TempBanCommand extends SinkCommand {

    private MySQLDatabase db;

    public TempBanCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
        getCommandOptions().setIrcOpOnly(true);
    }

    public static String getFinalArg(final String[] args, final int start) {
        final StringBuilder bldr = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i != start) {
                bldr.append(" ");
            }
            bldr.append(args[i]);
        }
        return bldr.toString();
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        IngameUser target = SinkLibrary.getInstance().getIngameUser(args[0], false);

        String targetName = target.isOnline() ? target.getName() : args[0];

        String prefix = BukkitUtil.getSenderName(sender);

        final String time = getFinalArg(args, 1);
        final long unbantimestamp;
        try {
            unbantimestamp = DateUtil.parseDateDiff(time, true);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + time + " ist keine gueltige Zeitangabe!");
            return true;
        }

        long bantime = unbantimestamp - System.currentTimeMillis();
        long maxtime = 2 * 60 * 60 * 1000;

        if(!sender.hasPermission("sinkbans.ban") && bantime > maxtime) {
            sender.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + "Du kannst nur für maximal 2 Stunden bannen!");
            return true;
        }

        String reason = ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(unbantimestamp);
        String reasonPrefix = ChatColor.DARK_RED + "Gesperrt: ";

        if (target.isOnline()) {
            target.getPlayer().kickPlayer(reasonPrefix + reason);
        }

        try {
            db.unbanTempBan(target.getUniqueId(), target.getName(), unbantimestamp); // Unban all bans done before
            db.tempBan(target.getName(), unbantimestamp, sender.getName(), target.getUniqueId(), BanType.MANUAL_BAN);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat " + ChatColor.RED + targetName + ChatColor.GOLD + " gesperrt: " + reason.trim();
        BukkitUtil.broadcast(msg, "sinkbans.notification", false);
        if (sender instanceof IrcCommandSender) {
            sender.sendMessage(msg);
        }

        return true;
    }
}
