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

import de.static_interface.sinkbans.MySQLDatabase;
import de.static_interface.sinkbans.model.BanRequestData;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.exception.UserNotFoundException;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;

public class BanRequestCommand  extends SinkCommand {
    private MySQLDatabase db;
    public BanRequestCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        if(args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /banrequest <new|list|cancel|accept|deny>");
            return true;
        }
        switch(args[0].toLowerCase().trim()) {
            case "new": {
                if(args.length < 2) {
                    sender.sendMessage(ChatColor.RED +"Usage: /banrequest new <name> [reason]");
                    return true;
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                if(target == null || StringUtil.isEmptyOrNull(target.getDisplayName())) {
                    throw new UserNotFoundException(args[1]);
                }
                String reason = null;
                if(args.length >= 3) {
                    for (int i = 2; i < args.length; i++) {
                        if(reason == null) {
                            reason = args[i];
                            continue;
                        }

                        reason += " " + args[i];
                    }
                }

                int id;
                try {
                    id = db.createBanRequest(sender, target, reason, -1);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                String msg =sender.getName() + ChatColor.DARK_RED + " hat einen Bannantrag für " + target.getDisplayName() + ChatColor.DARK_RED + " erstellt." + (reason == null ? "" : " Grund: "  + ChatColor.RED + reason + ".") + ChatColor.GRAY + " ("  + ChatColor.GOLD + "#"  + id + ChatColor.GRAY + ")";
                String perm = "sinkbans.banrequestnotification";
                BukkitUtil.broadcast(msg, perm, false);
                if(!sender.hasPermission(perm)) {
                    sender.sendMessage(msg);
                }
                break;
            }

            case "list": {
                sender.sendMessage(ChatColor.GOLD + "--- Ban Requests ---");
                List<BanRequestData> banRequests;
                try {
                    banRequests = db.getAllBanRequestData();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                int i = 0;
                for(BanRequestData request : banRequests) {
                    if(request.state != MySQLDatabase.RequestState.PENDING) continue;
                    String reason = request.reason != null ? request.reason : "Du wurdest permanent gesperrt";
                    sender.sendMessage(ChatColor.GOLD + "#" + request.id + " " + request.creatorName + ChatColor.GRAY + " -> " + request.target.getDisplayName() + ChatColor.GRAY + ": " + ChatColor.RED + reason);
                    i++;
                }

                if(i == 0) {
                    sender.sendMessage(ChatColor.GREEN + "No requests found :)");
                    break;
                }
                break;
            }

            case "cancel": {
                if(args.length < 2) {
                    sender.sendMessage(ChatColor.RED +"Usage: /banrequest accept <id>");
                    return true;
                }
                int id = Integer.valueOf(args[1]);
                BanRequestData request;
                try {
                    request = db.getBanRequest(id);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if(request == null) {
                    sender.sendMessage(ChatColor.RED + "Banrequest " + ChatColor.GOLD + "#" + id + " " + ChatColor.RED + " not found!");
                    return true;
                }

                if(!sender.hasPermission("sinkbans.closeothers")) {
                    if ((request.creator != null && sender instanceof Player && !((Player) sender).getUniqueId()
                            .equals(request.creator.getUniqueId()))
                        || (request.creator != null && !(sender instanceof Player)) || (request.creator == null && sender instanceof Player)
                        || (request.creator == null && !(sender instanceof Player)) && !request.creator.getDisplayName().equals(ChatColor.stripColor(
                            sender.getName()))) {
                        sender.sendMessage(ChatColor.RED + "This is not your banrequest");
                        return true;
                    }
                }

                long time = System.currentTimeMillis();
                try {
                    db.closeBanRequest(sender, request, MySQLDatabase.RequestState.CANCELLED, time);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                sender.sendMessage(ChatColor.DARK_GREEN + "Dein Antrag " + ChatColor.GOLD + "#" + id + " " + ChatColor.DARK_GREEN + " wurde abgebrochen!");
                break;
            }

            case "accept": {
                if(!sender.hasPermission("sinkbans.acceptbanrequest")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission");
                    return true;
                }
                if(args.length < 2) {
                    sender.sendMessage(ChatColor.RED +"Usage: /banrequest accept <id>");
                    return true;
                }
                int id = Integer.valueOf(args[1]);
                BanRequestData request;
                try {
                    request = db.getBanRequest(id);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if(request == null) {
                    sender.sendMessage(ChatColor.RED + "Banrequest " + ChatColor.GOLD + "#" + id + " " + ChatColor.RED + " not found!");
                    return true;
                }

                long time = System.currentTimeMillis();
                try {
                    db.closeBanRequest(sender, request, MySQLDatabase.RequestState.ACCEPTED, time);
                    db.ban(request.target.getDisplayName(), request.reason, sender.getName(), request.target.getUniqueId(), BanType.BANREQUEST, time);
                    if(request.target.isOnline()) {
                        request.target.getPlayer().kickPlayer(ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + request.reason);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                SinkUser user = SinkLibrary.getInstance().getUser(sender);
                String msg = user.getDisplayName() + ChatColor.RED + " hat den Antrag von " + request.creatorName + ChatColor.RED + " für " + request.target.getDisplayName() + ChatColor.RED + " angenommen.";
                String perm = "sinkbans.banrequestacceptednotification";
                BukkitUtil.broadcast(msg, perm, false);
                if(!sender.hasPermission(perm)) {
                    sender.sendMessage(msg);
                }
                if(request.creator != null && request.creator.isOnline()) {
                    request.creator.sendMessage(ChatColor.DARK_GREEN + "Dein Antrag "  + ChatColor.GOLD + "#" + id + " " + ChatColor.DARK_GREEN + " wurde angenommen!");
                }
                break;
            }

            case "deny": {
                if(!sender.hasPermission("sinkbans.denybanrequest")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission");
                    return true;
                }
                if(args.length < 2) {
                    sender.sendMessage(ChatColor.RED +"Usage: /banrequest deny <id>");
                    return true;
                }
                int id = Integer.valueOf(args[1]);
                BanRequestData request;
                try {
                    request = db.getBanRequest(id);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if(request == null) {
                    sender.sendMessage(ChatColor.RED + "Banrequest " + ChatColor.GOLD + "#" + id + " " + ChatColor.RED + " not found!");
                    return true;
                }

                long time = System.currentTimeMillis();
                try {
                    db.closeBanRequest(sender, request, MySQLDatabase.RequestState.DENIED, time);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ChatColor.DARK_RED + "Du hast den Antrag "  + ChatColor.GOLD + "#" + id + " " + ChatColor.DARK_RED+ " abgelehnt!");
                if(request.creator != null && request.creator.isOnline()) {
                    request.creator.sendMessage(ChatColor.DARK_RED + "Dein Antrag "  + ChatColor.GOLD + "#" + id + " " + ChatColor.DARK_RED+ " wurde abgelehnt!");
                }
                break;
            }

            default: {
                sender.sendMessage(ChatColor.RED +"Usage: /banrequest <new|list|cancel|accept|deny>");
                return true;
            }
        }

        return true;
    }
}
