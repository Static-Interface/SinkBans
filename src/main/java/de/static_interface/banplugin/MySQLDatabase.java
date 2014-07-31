package de.static_interface.banplugin;

import de.static_interface.sinklibrary.BukkitUtil;
import org.bukkit.ChatColor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MySQLDatabase
{
    private static final String LOG_TABLE = "iplog";
    public static final String MULTIACCOUNT_TABLE = "multiacc";
    private Connection connection;
    public static final String PLAYER_TABLE = "players";
    public static final String IP_TABLE = "ips";
    final String driver = "com.mysql.jdbc.Driver";

    public MySQLDatabase(String hostname, String port, String database, String user, String password) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Class.forName(driver).newInstance();
        connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s", hostname, port), user, password);
        execute("CREATE DATABASE IF NOT EXISTS " + database);
        connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s", hostname, port, database), user, password);
        prepare();
    }

    public void prepare() throws SQLException
    {
        String query = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "playername VARCHAR(16) NOT NULL, " +
                "reason TEXT CHARACTER SET utf8, " +
                "bantimestamp BIGINT NOT NULL, " +
                "unbantimestamp BIGINT NOT NULL, " +
                "isbanned TINYINT(0) NOT NULL, " +
                "bannedby VARCHAR(16) NOT NULL, "+
                "unbannedby VARCHAR(16), " +
                "uuid VARCHAR(36) NOT NULL" + //16 Bytes * 2 + 4
                ");", PLAYER_TABLE);
        execute(query);

        query = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "ip VARCHAR(15) NOT NULL, " +
                "bantimestamp BIGINT NOT NULL, " +
                "unbantimestamp BIGINT NOT NULL, " +
                "isbanned TINYINT(0) NOT NULL, " +
                "bannedby VARCHAR(16) NOT NULL, "+
                "unbannedby VARCHAR(16)" +
                ");", IP_TABLE);
        execute(query);

        query = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "uuid VARCHAR(36) NOT NULL, " + //16 Bytes * 2 + 4
                "playername VARCHAR(16) NOT NULL, " +
                "ip VARCHAR(15) NOT NULL, " +
                "timestamp BIGINT NOT NULL " +
                ");", LOG_TABLE);

        execute(query);

        query = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "uuid VARCHAR(36) NOT NULL, " + //16 Bytes * 2 + 4
                "playername VARCHAR(16) NOT NULL " +
                ");", MULTIACCOUNT_TABLE);

        execute(query);
    }

    public void execute(String query) throws SQLException
    {
        PreparedStatement statment = connection.prepareStatement(query);
        statment.execute();
    }

    private ResultSet executeQuery(String query) throws SQLException
    {
        PreparedStatement statment = connection.prepareStatement(query);
        return statment.executeQuery();
    }

    //public ArrayList<BanData> getBanData(String playername, String ip) throws SQLException
    //{
    //    ResultSet result = executeQuery(String.format("SELECT * FROM %s", PLAYER_TABLE)); // WHERE playername = '%s';", PLAYER_TABLE, playername));
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
    //    result = executeQuery(String.format("SELECT * FROM %s", IP_TABLE)); // WHERE ip = '%s';", IP_TABLE, ip));
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

    public void fixOldBans() throws SQLException
    {
        ResultSet result = executeQuery(String.format("SELECT * FROM %s WHERE uuid=NULL", PLAYER_TABLE));
        List<BanData> entries = getFromResultSet(result, false);

        for(BanData entry : entries)
        {
            UUID uuid = BukkitUtil.getUUIDByName(entry.getName());
            execute(String.format("UPDATE %s SET uuid = '%s' WHERE playername = '%s'", PLAYER_TABLE, uuid.toString(), entry.getName()));
        }
    }

    public List<BanData> getBanData(String searchString, boolean isIp) throws SQLException
    {
        String query = isIp
                ? String.format("SELECT * FROM %s WHERE ip = '%s';", IP_TABLE, searchString)
                : String.format("SELECT * FROM %s WHERE uuid = '%s';", PLAYER_TABLE, searchString);

        ResultSet result = executeQuery(query);
        return getFromResultSet(result, isIp);
    }


    public List<BanData> getOldBanData(String playername) throws SQLException
    {
        String query = String.format("SELECT * FROM %s WHERE playername = '%s';", PLAYER_TABLE, playername);;
        ResultSet result = executeQuery(query);
        return getFromResultSet(result, false);
    }

    public List<BanData> getFromResultSet(ResultSet resultSet, boolean isIp) throws SQLException
    {
        ArrayList<BanData> tmp = new ArrayList<>();
        while (resultSet.next())
        {
            try
            {
                BanData data = new BanData(isIp ? resultSet.getString("ip") : resultSet.getString("playername"));
                data.setBanner(resultSet.getString("bannedby"));
                data.setReason(resultSet.getString("reason"));
                data.setBanTimeStamp(resultSet.getLong("bantimestamp"));
                data.setUnbanTimeStamp(resultSet.getLong("unbantimestamp"));
                if (isIp) data.setIp(resultSet.getString("ip"));
                data.setBanned(resultSet.getBoolean("isbanned"));
                data.setUniqueId(resultSet.getString("uuid"));
                tmp.add(data);
            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    public void ban(String playername, String reason, String bannedby, UUID uuid) throws SQLException
    {
        long banTimeStamp = System.currentTimeMillis();
        reason = reason.trim();
        reason = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', reason));
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s','%s','%s', '-1', '1', '%s', NULL, '%s')", PLAYER_TABLE, playername, reason, banTimeStamp, bannedby, uuid.toString()));
    }

    public void tempBan(String playername, long unbanTimeStamp, String bannedby, UUID uuid) throws SQLException
    {
        long banTimeStamp = System.currentTimeMillis();
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s', NULL, '%s', '%s', '1', '%s', NULL, '%s')",
                    PLAYER_TABLE, playername, banTimeStamp, unbanTimeStamp, bannedby, uuid.toString()));
    }

    public void banIp(String ip, String bannedby) throws SQLException
    {
        long banTimeStamp = System.currentTimeMillis();
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s', '%s', '-1', '1', '%s', NULL)",
                    IP_TABLE, ip, banTimeStamp, bannedby));
    }

    public void unban(UUID uuid, String playername, String unbannedby) throws SQLException
    {
        long unbanTimeStamp = System.currentTimeMillis();
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = '%s', unbantimestamp = %s WHERE isbanned = 1 AND uuid = '%s';" , PLAYER_TABLE, unbannedby, unbanTimeStamp, uuid.toString()));
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = '%s', unbantimestamp = %s WHERE isbanned = 1 AND playername = '%s';" , PLAYER_TABLE, unbannedby, unbanTimeStamp, playername));
    }

    public void unbanTempBan(UUID uuid, String playername, long unbanTimeStamp) throws SQLException
    {
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = NULL, unbantimestamp = '%s' WHERE isbanned = 1 AND uuid = '%s';" , PLAYER_TABLE, unbanTimeStamp, uuid.toString()));
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = NULL, unbantimestamp = '%s' WHERE isbanned = 1 AND playername = '%s';" , PLAYER_TABLE, unbanTimeStamp, playername));
    }

    public void unbanIp(String ip, String unbannedby) throws SQLException
    {
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = '%s', unbantimestamp = '%s' WHERE ip = '%s';", IP_TABLE, unbannedby, System.currentTimeMillis(), ip));
    }

    public void logIp(UUID uniqueId, String playername, String ip) throws SQLException
    {
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s', '%s', '%s', '%s')", LOG_TABLE, uniqueId.toString(), playername, ip, System.currentTimeMillis()));
    }

    public void fixBan(BanData data, UUID uniqueId) throws SQLException
    {
        execute(String.format("UPDATE %s SET uuid = '%s' WHERE playername = '%s'", PLAYER_TABLE, uniqueId.toString(), data.getName()));
    }

    public List<Account> getAccounts(String ip) throws SQLException
    {
        ResultSet resultSet = executeQuery(String.format(
                "SELECT uuid, " + LOG_TABLE + ".playername, " + LOG_TABLE +".ip FROM " + LOG_TABLE + " INNER JOIN(SELECT ip, playername " +
                "FROM " + LOG_TABLE + " WHERE " + LOG_TABLE + ".ip = '%s' GROUP BY ip HAVING count(id) > 1) dup " +
                "ON " + LOG_TABLE + ".ip = dup.ip AND " + LOG_TABLE + ".playername != dup.playername GROUP BY playername", ip));

        ArrayList<Account> tmp = new ArrayList<>();
        while (resultSet.next())
        {
            try
            {
                Account acc = new Account(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("playername"));
                tmp.add(acc);
            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    public void addMultiAccount(Account account) throws SQLException
    {
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s', '%s')", MULTIACCOUNT_TABLE, account.getUniqueId().toString(), account.getPlayername()));
    }

    public boolean isAllowedMultiAccount(Account account) throws SQLException
    {
        ResultSet set = executeQuery(String.format("SELECT * FROM %s WHERE uuid = '%s'", MULTIACCOUNT_TABLE, account.getUniqueId().toString()));
        return set.next();
    }
}
