package de.static_interface.sinkbans;

import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinkbans.model.BanData;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MySQLDatabase {

    public static final String LOG_TABLE = "iplog";
    public static final String MULTIACCOUNT_TABLE = "multiacc";
    public static final String PLAYER_TABLE = "players";
    public static final String IP_TABLE = "ips";
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
            statment.setObject(i, s);
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
        if (uuid == null) {
            throw new IllegalStateException("uuid may not be null!");
        }
        if (type == BanType.AUTO_MULTI_ACC) {
            return;
        }
        long banTimeStamp = System.currentTimeMillis();
        if (reason != null) {
            reason = reason.trim();
            reason = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', reason));
        }
        execute(String.format("INSERT INTO %s VALUES(NULL, ?, ?, ?, '-1', '1', ?, NULL, ?, ?)",
                              PLAYER_TABLE), playername, reason, banTimeStamp, bannedby, uuid.toString(), type);
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
}
