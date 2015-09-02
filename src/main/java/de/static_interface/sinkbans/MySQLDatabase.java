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
import de.static_interface.sinkbans.model.BanRequestData;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.user.Identifiable;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class MySQLDatabase {

    public static final String LOG_TABLE = "iplog";
    public static final String MULTIACCOUNT_TABLE = "multiacc";
    public static final String PLAYER_TABLE = "players";
    public static final String IP_TABLE = "ips";
    public static final String BANREQUEST_TABLE = "banrequest";

    final String driver = "com.mysql.jdbc.Driver";
    private Connection connection;

    public MySQLDatabase(String hostname, String port, String database, String user, String password)
            throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class.forName(driver).newInstance();
        connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s", hostname, port), user, password);
        execute("CREATE DATABASE IF NOT EXISTS " + database);
        SinkLibrary.getInstance().getCustomLogger().debug("CREATE DATABASE IF NOT EXISTS " + database);
        connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s", hostname, port, database), user, password);
        prepare();
    }

    public void prepare() throws SQLException {
        String query = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "playername VARCHAR(16) NOT NULL, " +
                "reason TEXT CHARACTER SET utf8, " +
                "bantimestamp BIGINT NOT NULL, " +
                "unbantimestamp BIGINT NOT NULL, " +
                "isbanned TINYINT(0) NOT NULL, " +
                "bannedby VARCHAR(16), " +
                "unbannedby VARCHAR(16), " +
                "uuid VARCHAR(36) NOT NULL," + //16 Bytes * 2 + 4
                "bantype INTEGER NOT NULL" +
                ");", PLAYER_TABLE);
        execute(query);

        query = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "ip VARCHAR(15) NOT NULL, " +
                "bantimestamp BIGINT NOT NULL, " +
                "unbantimestamp BIGINT NOT NULL, " +
                "isbanned TINYINT(0) NOT NULL, " +
                "bannedby VARCHAR(16) NOT NULL, " +
                "unbannedby VARCHAR(16)" +
                ");", IP_TABLE);
        execute(query);

        query = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "uuid VARCHAR(36) NOT NULL, " + //16 Bytes * 2 + 4
                "playername VARCHAR(16) NOT NULL, " +
                "ip VARCHAR(15) NOT NULL, " +
                "timestamp BIGINT NOT NULL " +
                ");", LOG_TABLE);

        execute(query);

        query = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "uuid VARCHAR(36) NOT NULL, " + //16 Bytes * 2 + 4
                "playername VARCHAR(16) NOT NULL " +
                ");", MULTIACCOUNT_TABLE);
        execute(query);

        query = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "creator VARCHAR(16) NOT NULL, " +
                "creator_uuid VARCHAR(36), " +
                "reason VARCHAR(255), " +
                "target_uuid VARCHAR(36) NOT NULL, " +
                "closer VARCHAR(16), " +
                "closer_uuid VARCHAR(36)," +
                "state INT NOT NULL, " +
                "time_created BIGINT NOT NULL, " +
                "time_closed BIGINT, " +
                "unbantime BIGINT NOT NULL);", BANREQUEST_TABLE);
        execute(query);
    }

    public void execute(String query, Object... paramObjects) throws SQLException {
        PreparedStatement statment = connection.prepareStatement(query);
        int i = 1;
        for (Object s : paramObjects) {
            statment.setObject(i, s);
            i++;
        }
        statment.execute();
    }

    public ResultSet executeQuery(String query, Object... paramObjects) throws SQLException {
        PreparedStatement statment = connection.prepareStatement(query);
        int i = 1;
        for (Object s : paramObjects) {
            statment.setObject(i, s == null ? null : s.toString());
            i++;
        }
        return statment.executeQuery();
    }

    //public ArrayList<BanData> getBanData(String playername, String ip) throws SQLException
    //{
    //    ResultSet result = executeQuery(String.format("SELECT * FROM %s", PLAYER_TABLE)); // WHERE playername = %s;", PLAYER_TABLE, playername));
    //    ArrayList<BanData> entries = new ArrayList<>();
    //    BanData data = getFromResultSet(result, playername, false);
    //    if (data != null)
    //        entries.add(data);
    //    while(result.next())
    //    {
    //        data = getFromResultSet(result, playername, false);
    //        if (data != null)
    //            entries.add(data);
    //    }
    //    result = executeQuery(String.format("SELECT * FROM %s", IP_TABLE)); // WHERE ip = %s;", IP_TABLE, ip));
    //    data = getFromResultSet(result, true);
    //    if (data != null)
    //        entries.add(data);
    //
    //    while(result.next())
    //    {
    //        data = getFromResultSet(result, true).get(0);
    //        if (data != null)
    //            entries.add(data);
    //    }
    //
    //    return entries;
    //}

    public void fixOldBans() throws SQLException {
        ResultSet result = executeQuery(String.format(
                "SELECT * FROM %s WHERE uuid=NULL", PLAYER_TABLE));
        List<BanData> entries = getFromResultSet(result, false);

        for (BanData entry : entries) {
            UUID uuid = BukkitUtil.getUniqueIdByName(entry.getName());
            execute(String.format("UPDATE %s SET uuid = ? WHERE playername = ?",
                                  PLAYER_TABLE),
                    uuid.toString(), entry.getName());
        }
    }

    public List<BanData> getBanData(String searchString, boolean isIp) throws SQLException {
        String query = isIp
                       ? String.format("SELECT * FROM %s WHERE ip = ? "
                                       + "ORDER BY bantimestamp DESC", IP_TABLE)
                       : String.format("SELECT * FROM %s WHERE uuid = ? "
                                       + "ORDER BY bantimestamp DESC", PLAYER_TABLE);

        ResultSet result = executeQuery(query, searchString);
        return getFromResultSet(result, isIp);
    }

    public List<BanRequestData> getAllBanRequestData() throws SQLException {
        String query = String.format("SELECT * FROM %s", BANREQUEST_TABLE);
        ResultSet result = executeQuery(query);
        return getRequestsFromResultSet(result);
    }

    public BanRequestData getBanRequest(int id) throws SQLException {
        String query = String.format("SELECT * FROM %s WHERE id = ?", BANREQUEST_TABLE);
        ResultSet result = executeQuery(query, id);
        List<BanRequestData> data = getRequestsFromResultSet(result);
        if(data.size() == 0) return null;
        return data.get(0);
    }

    private List<BanRequestData> getRequestsFromResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<BanRequestData> tmp = new ArrayList<>();
        while (resultSet.next()) {
            try {
                BanRequestData data = new BanRequestData();
                data.id = resultSet.getInt("id");
                data.creatorName = resultSet.getString("creator");
                String uuid = resultSet.getString("creator_uuid");
                data.creator = uuid != null ? SinkLibrary.getInstance().getIngameUser(UUID.fromString(uuid)) : null;

                uuid = resultSet.getString("target_uuid");
                data.target = SinkLibrary.getInstance().getIngameUser(UUID.fromString(uuid));

                data.reason = resultSet.getString("reason");

                data.closerName = resultSet.getString("closer");
                uuid = resultSet.getString("closer_uuid");
                data.closer = uuid != null ? SinkLibrary.getInstance().getIngameUser(UUID.fromString(uuid)) : null;

                data.timeCreated = resultSet.getLong("time_created");
                data.timeClosed = resultSet.getLong("time_closed");

                data.state = resultSet.getInt("state");
                tmp.add(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(tmp);
        return tmp;
    }

    public List<BanData> getOldBanData(String playername) throws SQLException {
        String query = String.format("SELECT * FROM %s WHERE playername = ?;",
                                     PLAYER_TABLE);
        ResultSet result = executeQuery(query, playername);
        return getFromResultSet(result, false);
    }

    public List<BanData> getFromResultSet(ResultSet resultSet, boolean isIp) throws SQLException {
        ArrayList<BanData> tmp = new ArrayList<>();
        while (resultSet.next()) {
            try {
                BanData data = new BanData(isIp ? resultSet.getString("ip") : resultSet.getString("playername"));
                data.setBanner(resultSet.getString("bannedby"));
                if (!isIp) {
                    data.setReason(resultSet.getString("reason"));
                }
                data.setBanTimeStamp(resultSet.getLong("bantimestamp"));
                data.setUnbanTimeStamp(resultSet.getLong("unbantimestamp"));
                if (isIp) {
                    data.setIp(resultSet.getString("ip"));
                }
                data.setBanned(resultSet.getBoolean("isbanned"));
                if (!isIp) {
                    data.setUniqueId(resultSet.getString("uuid"));
                }

                tmp.add(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(tmp);
        return tmp;
    }
    public void ban(String playername, String reason, String bannedby, UUID uuid, int type) throws SQLException {
        ban(playername, reason, bannedby, uuid, type, System.currentTimeMillis());
    }
    public void ban(String playername, String reason, String bannedby, UUID uuid, int type, long time) throws SQLException {
        if (uuid == null) {
            throw new IllegalStateException("uuid may not be null!");
        }
        if (type == BanType.AUTO_MULTI_ACC) {
            return;
        }
        if (reason != null) {
            reason = reason.trim();
            reason = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', reason));
        }
        execute(String.format("INSERT INTO %s VALUES(NULL, ?, ?, ?, '-1', '1', ?, NULL, ?, ?)",
                              PLAYER_TABLE), playername, reason, time, bannedby, uuid.toString(), type);
    }

    public void tempBan(String playername, long unbanTimeStamp, String bannedby, UUID uuid, int type) throws SQLException {
        if (uuid == null) {
            throw new IllegalStateException("uuid may not be null!");
        }
        long banTimeStamp = System.currentTimeMillis();
        execute(String.format("INSERT INTO %s VALUES(NULL, ?, NULL, ?, ?, '1', ?, NULL, ?, ?)",
                              PLAYER_TABLE), playername, String.valueOf(banTimeStamp),
                unbanTimeStamp, bannedby, uuid.toString(), type);
    }

    public void banIp(String ip, String bannedby) throws SQLException {
        long banTimeStamp = System.currentTimeMillis();
        execute(String.format("INSERT INTO %s VALUES(NULL, ?, ?, '-1', '1', ?, NULL)",
                              IP_TABLE), ip, banTimeStamp, bannedby);
    }

    public void unban(UUID uuid, String playername, String unbannedby) throws SQLException {
        long unbanTimeStamp = System.currentTimeMillis();
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = ?, "
                              + "unbantimestamp = ? WHERE isbanned = 1 AND uuid = ?;", PLAYER_TABLE),
                unbannedby, unbanTimeStamp, uuid.toString());

        //Todo: remove on 1.8?
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = ?, unbantimestamp = ? "
                              + "WHERE isbanned = 1 AND playername = ?;",
                              PLAYER_TABLE), unbannedby, unbanTimeStamp, playername);
    }

    public void unbanTempBan(UUID uuid, String playername, long unbanTimeStamp) throws SQLException {
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = NULL, "
                              + "unbantimestamp = ? WHERE isbanned = 1 AND uuid = ?;",
                              PLAYER_TABLE), unbanTimeStamp, uuid.toString());

        //Todo: remove on 1.8?
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = NULL, "
                              + "unbantimestamp = ? WHERE isbanned = 1 AND playername = ?;",
                              PLAYER_TABLE), unbanTimeStamp, playername);
    }

    public void unbanIp(String ip, String unbannedby) throws SQLException {
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = ?, "
                              + "unbantimestamp = ? WHERE ip = ?;",
                              IP_TABLE), unbannedby, System.currentTimeMillis(), ip);
    }

    public void logIp(UUID uniqueId, String playername, String ip) throws SQLException {
        execute(String.format("INSERT INTO %s VALUES(NULL, ?, ?, ?, ?)", LOG_TABLE), uniqueId.toString(),
                playername, ip, System.currentTimeMillis());
    }

    public void fixBan(BanData data, UUID uniqueId) throws SQLException {
        execute(String.format("UPDATE %s SET uuid = ? WHERE playername = ?", PLAYER_TABLE), uniqueId.toString(), data.getName());
    }

    public List<Account> getAccounts(String ip) throws SQLException {
        ResultSet resultSet = executeQuery(String.format(
                "SELECT * from %s WHERE ip = ?", LOG_TABLE), ip);

        ArrayList<Account> tmp = new ArrayList<>();
        while (resultSet.next()) {
            try {
                Account acc = new Account(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("playername"));
                if (acc.getPlayername().equalsIgnoreCase("Player")) {
                    continue;
                }
                if (containsAccount(tmp, acc)) {
                    continue;
                }
                tmp.add(acc);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }


    public int countIps(String ip, int hours) throws SQLException {
        try {
            ResultSet resultSet = executeQuery(String.format(
                    "SELECT * from %s WHERE ip = ? AND timestamp > ?", LOG_TABLE), ip, System.currentTimeMillis() - hours * 60 * 60 * 1000);

            int i = 0;
            while (resultSet.next()) {
                i++;
            }

            return i;
        } catch (Exception e) {
            return -1;
        }
    }

    public long getFirstJoin(Account account, String ip) throws SQLException {
        try {
            ResultSet resultSet = executeQuery(String.format(
                    "SELECT * from %s WHERE ip = ?", LOG_TABLE), ip);

            long minValueIp = getMinValue(resultSet, "timestamp");

            resultSet = executeQuery(String.format(
                    "SELECT * from %s WHERE uuid = ?", LOG_TABLE), account.getUniqueId().toString());

            long minValueAccount = getMinValue(resultSet, "timestamp");

            return minValueIp < minValueAccount ? minValueIp : minValueAccount;
        } catch (Exception e) {
            return -1;
        }
    }

    private long getMinValue(ResultSet resultSet, String column) throws SQLException {
        long minTime = Long.MAX_VALUE;
        while(resultSet.next()) {
            long joinTime = resultSet.getLong(column);
            if(joinTime < minTime) {
                minTime = joinTime;
            }
        }
        if(minTime == Long.MAX_VALUE) return 0;

        return minTime;
    }

    private boolean containsAccount(ArrayList<Account> tmp, Account acc) {
        for (Account account : tmp) {
            if (account.getUniqueId().toString().equals(acc.getUniqueId().toString())) {
                return true;
            }
        }
        return false;
    }

    public void addMultiAccount(Account account) throws SQLException {
        execute(String.format("INSERT INTO %s VALUES(NULL, ?, ?)", MULTIACCOUNT_TABLE), account.getUniqueId().toString(), account.getPlayername());
    }

    public boolean isAllowedMultiAccount(Account account) throws SQLException {
        boolean allowed;
        ResultSet set = executeQuery(String.format("SELECT * FROM %s WHERE uuid = ?",
                                                   MULTIACCOUNT_TABLE), account.getUniqueId().toString());
        allowed = set.next();

        //if(!allowed)
        //{
        //    set = executeQuery(String.format("SELECT * FROM %s WHERE uuid = %s", MULTIACCOUNT_TABLE, account.getUniqueId().toString()));
        //}
        //allowed = set.next();
        return allowed;
    }

    public int createBanRequest(CommandSender sender, IngameUser target, @Nullable String reason, long unbantimestamp) throws SQLException {
        String creator = sender.getName();
        String creatorUuid = (sender instanceof Player) ? ((Player)sender).getUniqueId().toString() : null;
        String targetUuid = target.getUniqueId().toString();
        long timeCreate = System.currentTimeMillis();
        execute(String.format("INSERT INTO %s VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", BANREQUEST_TABLE), creator, creatorUuid, reason, targetUuid, null, null,  RequestState.PENDING, timeCreate, -1, unbantimestamp);
        ResultSet rs =  executeQuery(String.format("SELECT * FROM %s WHERE time_created = ?", BANREQUEST_TABLE), timeCreate);
        rs.next();
        return rs.getInt("id");
    }

    public void closeBanRequest(CommandSender sender, BanRequestData request, int state, long time) throws SQLException {
        if(state == RequestState.PENDING) throw new IllegalArgumentException("Can't use PENDING state for closing!");
        SinkUser user = SinkLibrary.getInstance().getUser((Object)sender);
        String closer = sender.getName();
        String closer_uuid = user instanceof Identifiable ? ((Identifiable)user).getUniqueId().toString() : null;
        execute(String.format("UPDATE %s SET closer = ?, closer_uuid = ?, state = ?, time_closed = ? WHERE id = ?", BANREQUEST_TABLE), closer, closer_uuid, state, time, request.id);
    }

    public static class RequestState {
        public static final int PENDING = 0;
        public static final int ACCEPTED = 1;
        public static final int DENIED = 2;
        public static final int CANCELLED = 3;
    }
}
