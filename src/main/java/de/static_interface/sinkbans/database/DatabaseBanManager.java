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

package de.static_interface.sinkbans.database;

import static de.static_interface.sinklibrary.database.query.Query.eq;
import static de.static_interface.sinklibrary.database.query.Query.from;

import de.static_interface.sinkbans.BuildFlags;
import de.static_interface.sinkbans.SinkBans;
import de.static_interface.sinkbans.Util;
import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.Debug;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DatabaseBanManager {
    public static void ban(IngameUser user, String reason, String bannerName) {
        BanRow row = new BanRow();
        row.bannedby = bannerName;
        row.bantimestamp = System.currentTimeMillis();
        row.isbanned = true;
        row.playername = user.getName();
        row.uuid = user.getUniqueId().toString();
        row.reason = reason;
        SinkBans.getInstance().getBanTable().insert(row);
        if(user.isOnline()) {
            user.getPlayer().kickPlayer("Gesperrt: " + row.reason);
        }
    }

    public static void unban(UUID uniqueId, String unbanner) {
        Debug.logMethodCall(uniqueId, unbanner);
        for(BanRow row : getUserBans(SinkLibrary.getInstance().getIngameUser(uniqueId))){
            if(!row.isbanned) continue;
            from(SinkBans.getInstance().getBanTable())
                    .update()
                    .set("unbannedby", "?")
                    .set("isbanned", "?")
                    .set("unbantimestamp", "?")
                    .where("id", eq("?"))
                    .execute(unbanner, false, System.currentTimeMillis(), row.id);
        }
    }

    public static void unbanTempBan(UUID uniqueId, String unbanner) {
        Debug.logMethodCall(uniqueId, unbanner);
        unbanTempBan(uniqueId, unbanner, System.currentTimeMillis());
    }

    public static void unbanTempBan(UUID uniqueId, String unbanner, long unbantimestamp) {
        Debug.logMethodCall(uniqueId, unbanner, unbantimestamp);
        for(BanRow row : getUserBans(SinkLibrary.getInstance().getIngameUser(uniqueId))){
            if(!row.isbanned) continue;
            if(row.unbantimestamp == -1) continue; //perm ban
            from(SinkBans.getInstance().getBanTable())
                    .update()
                    .set("unbannedby", "?")
                    .set("isbanned", "?")
                    .set("unbantimestamp", "?")
                    .where("id", eq("?"))
                    .execute(unbanner, false, unbantimestamp, row.id);
        }

    }

    public static void banIp(String ip, String banner) {
        if(BuildFlags.ENABLE_IPTABLES){
            Util.runCommandAsync("sudo /usr/sbin/iptables -A INPUT -s " + ip + " -j DROP");
        }
        IpBanRow row = new IpBanRow();
        row.ip = ip;
        row.bantimestamp = System.currentTimeMillis();
        row.bannedby = banner;
        row.isbanned = true;
        SinkBans.getInstance().getIpBanTable().insert(row);
    }

    public static List<Account> getAccounts(String ip) {
        Session[] matchedSessions =
                from(SinkBans.getInstance().getSessionsTable())
                .select()
                .where("ip", eq("?"))
                .getResults(ip);

        List<Account> accounts = new ArrayList<>();
        for(Session sess : matchedSessions){
            Account acc = new Account(UUID.fromString(sess.uuid),   sess.playerName);
            if (acc.getPlayername().equalsIgnoreCase("Player")) {
                continue;
            }
            if (containsAccount(accounts, acc)) {
                continue;
            }
            accounts.add(acc);
        }
        return accounts;
    }

    private static boolean containsAccount(List<Account> accounts, Account acc) {
        for (Account account : accounts) {
            if (account.getUniqueId().toString().equals(acc.getUniqueId().toString())) {
                return true;
            }
        }
        return false;
    }

    public static List<BanRow> getUserBans(IngameUser user) {
        return Arrays.asList(from(SinkBans.getInstance().getBanTable())
            .select()
            .where("uuid", eq("?"))
            .getResults(user.getUniqueId().toString()));
    }

    public static void unbanIp(String ip, String unbanner) {
        if(BuildFlags.ENABLE_IPTABLES){
            Util.runCommandAsync("sudo /usr/sbin/iptables -D INPUT -s " + ip + " -j DROP");
        }

        from(SinkBans.getInstance().getIpBanTable())
                .update()
                .set("isbanned","?")
                .set("unbannedby", "?")
                .set("unbantimestamp", "?")
                .where("ip", eq("?"))
                .execute(false, unbanner, System.currentTimeMillis(), ip);
    }

    public static void tempBan(IngameUser user, long timeOut, String bannerName) {
        BanRow row = new BanRow();
        row.bannedby = bannerName;
        row.bantimestamp = timeOut;
        row.isbanned = true;
        row.playername = user.getName();
        row.uuid = user.getUniqueId().toString();
        //row.reason = reason;
        SinkBans.getInstance().getBanTable().insert(row);
        if(user.isOnline()) {
            user.getPlayer().kickPlayer("Gesperrt");
        }
    }


    public static List<IpBanRow> getUserBans(String ip) {
        return Arrays.asList(from(SinkBans.getInstance().getIpBanTable())
                                     .select()
                                     .where("ip", eq("?"))
                                     .getResults(ip));
    }

    public static List<BanRequest> getAllBanRequests() {
        return Arrays.asList(from(SinkBans.getInstance().getBanRequestsTable())
                                     .select()
                                     .getResults());
    }

    public static BanRequest createBanRequest(CommandSender creator, IngameUser target, String reason, int state) {
        BanRequest banRequest = new BanRequest();
        banRequest.creator = creator.getName();
        if(creator instanceof Player) {
            banRequest.creator_uuid = ((Player)creator).getUniqueId().toString();
        }
        banRequest.reason = reason;
        banRequest.state = state;
        banRequest.target_uuid = target.getUniqueId().toString();
        banRequest.time_created = System.currentTimeMillis();
        return SinkBans.getInstance().getBanRequestsTable().insert(banRequest);
    }

    public static BanRequest getBanRequest(int id) {
        return from(SinkBans.getInstance().getBanRequestsTable())
                .select()
                .where("id", eq("?"))
                .get(id);
    }

    public static void closeBanRequest(CommandSender sender, BanRequest request, int state, long time) {
        from(SinkBans.getInstance().getBanRequestsTable())
                .update()
                .set("closer", "?")
                .set("closed_uuid", "?")
                .set("state", "?")
                .set("time_closed", "?")
                .where("id", eq("?"))
                .execute(sender.getName(), sender instanceof Player ? ((Player)sender).getUniqueId().toString() : null, state, time, request.id);
    }

    public static boolean isAllowedMultiAccount(IngameUser user) {
        return true; //Todo
    }

    public static void addMultiAccount(IngameUser user) {
        //Todo
    }
}
