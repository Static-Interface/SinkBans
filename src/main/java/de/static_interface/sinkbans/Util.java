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

package de.static_interface.sinkbans;

import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinkbans.model.BanData;
import de.static_interface.sinklibrary.util.BukkitUtil;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class Util {

    public static final String IP_PATTERN = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";

    public static String getIp(InetAddress address) {
        return address.getHostAddress();
    }

    public static boolean isValidIp(String ip) {
        return Pattern.matches(IP_PATTERN, ip);
    }

    public static boolean isBanned(Account acc, MySQLDatabase db) {
        try {
            List<BanData> datas = db.getBanData(acc.getUniqueId().toString(), false);
            datas.addAll(db.getOldBanData(acc.getPlayername()));
            for (BanData data : datas) {
                if (data.isBanned()) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static UUID getUniqueId(String playername, MySQLDatabase database) {
        UUID uuid = null;

        try {
            ResultSet result = database.executeQuery(String.format(
                    "SELECT * FROM %s WHERE playername=?",
                    MySQLDatabase.LOG_TABLE), playername);
            if (result.next()) {
                uuid = UUID.fromString(result.getString("uuid"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (uuid == null) {
            uuid = BukkitUtil.getUniqueIdByName(playername);
        }
        return uuid;
    }
}
