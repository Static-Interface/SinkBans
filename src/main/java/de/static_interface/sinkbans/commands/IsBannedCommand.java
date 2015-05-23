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
import de.static_interface.sinkbans.Util;
import de.static_interface.sinkbans.model.BanData;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IsBannedCommand extends SinkCommand {

    private MySQLDatabase db;

    public IsBannedCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {

        List<BanData> datas = new ArrayList<>();

        if (args.length < 1) {
            return false;
        }

        String search = SinkLibrary.getInstance().getIngameUser(args[0]).getName();

        boolean ip = Util.isValidIp(search);

        String prefix = ip ? "Die IP " : "Spieler ";
        if (!ip) {
            try {
                search = Util.getUniqueId(search, db).toString();
                datas = db.getBanData(search, ip);
                datas.addAll(db.getOldBanData(args[0]));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        try {
            datas = db.getBanData(search, ip);
        } catch (SQLException ignored) {
        }

        Collections.sort(datas);

        for (BanData data : datas) {
            if (data.isBanned()) {
                String
                        reason =
                        data.isTempBanned() ? ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(data.getUnbanTimeStamp())
                                            : data.getReason();
                reason = reason.trim();
                sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + args[0] + ChatColor.GOLD +
                                   " ist gebannt! Gebannt von: " + ChatColor.DARK_RED + data.getBanner() + ChatColor.GOLD + ", Grund: " + reason);
                return true;
            }
        }

        sender.sendMessage(ChatColor.GOLD + prefix + ChatColor.RED + args[0] + ChatColor.GOLD + " ist nicht gebannt!");
        return true;
    }
}
