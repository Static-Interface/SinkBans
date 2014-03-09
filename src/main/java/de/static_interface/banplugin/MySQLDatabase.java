package de.static_interface.banplugin;

import org.bukkit.ChatColor;

import java.sql.*;
import java.util.ArrayList;

public class MySQLDatabase
{
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
                "playername VARCHAR(16) PRIMARY KEY NOT NULL, " +
                "reason TEXT CHARACTER SET utf8 NOT NULL, " +
                "bantimestamp BIGINT NOT NULL, " +
                "unbantimestamp BIGINT NOT NULL, " +
                "bannedby VARCHAR(16) NOT NULL"+
                ");", PLAYER_TABLE);
        execute(query);

        query = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "ip VARCHAR(15) PRIMARY KEY NOT NULL, " +
                "reason TEXT CHARACTER SET utf8 NOT NULL, " +
                "bantimestamp BIGINT NOT NULL, " +
                "bannedby VARCHAR(16) NOT NULL"+
                ");", IP_TABLE);
        execute(query);
    }

    private void execute(String query) throws SQLException
    {
        PreparedStatement statment = connection.prepareStatement(query);
        statment.execute();
    }

    private ResultSet executeQuery(String query) throws SQLException
    {
        PreparedStatement statment = connection.prepareStatement(query);
        return statment.executeQuery();
    }

    public ArrayList<BanData> getBanData(String playername, String ip) throws SQLException
    {
        ResultSet result = executeQuery(String.format("SELECT * FROM %s WHERE playername = '%s';", PLAYER_TABLE, playername));
        ArrayList<BanData> entries = new ArrayList<>();
        BanData data = getFromResultSet(result, playername);
        if (data != null)
            entries.add(data);
        while(result.next())
        {
            data = getFromResultSet(result, playername);
            if (data != null)
                entries.add(data);
        }
        result = executeQuery(String.format("SELECT * FROM %s WHERE ip = '%s';", IP_TABLE, ip));
        data = getFromResultSet(result, playername);
        if (data != null)
            entries.add(data);

        while(result.next())
        {
            data = getFromResultSet(result, playername);
            if (data != null)
                entries.add(data);
        }

        return entries;
    }

    public BanData getBanData(String searchString, boolean isIp) throws SQLException
    {
        ResultSet result = isIp
                ? executeQuery(String.format("SELECT * FROM %s WHERE ip = '%s';", IP_TABLE, searchString))
                : executeQuery(String.format("SELECT * FROM %s WHERE playername = '%s';", PLAYER_TABLE, searchString));
        return getFromResultSet(result, searchString);
    }

    public BanData getFromResultSet(ResultSet resultSet, String playername)
    {
        try
        {
            BanData data = new BanData(playername);
            data.setBanner(resultSet.getString("bannedby"));
            data.setReason(resultSet.getString("reason"));
            data.setBanTimeStamp(resultSet.getLong("bantimestamp"));
            data.setUnbanTimeStamp(resultSet.getLong("unbantimestamp"));
            data.setIp(resultSet.getString("ip"));
            return data;
        }
        catch(SQLException e)
        {
            return null;
        }
    }

    public void ban(String playername, String reason, String banner) throws SQLException
    {
        long banTimeStamp = System.currentTimeMillis();
        reason = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', reason));
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s','%s','%s', '-1', '%s')", PLAYER_TABLE, playername, reason, banTimeStamp, banner));
    }

    public void banIp(String ip, String banner) throws SQLException
    {
        long banTimeStamp = System.currentTimeMillis();
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s','%s', '%s')", IP_TABLE, ip, banTimeStamp, banner));
    }

    public void tempBan(String playername, long unbanTimestamp, String banner) throws SQLException
    {
        String reason = ChatColor.DARK_RED + "";
        long banTimeStamp = System.currentTimeMillis();
        execute(String.format("INSERT INTO %s VALUES(NULL, '%s', '%s', '%s','%s', '%s', '%s')", PLAYER_TABLE, playername, reason, banTimeStamp, reason, unbanTimestamp, banner));
    }

    public void unban(String playername) throws SQLException
    {
        execute(String.format("DELETE FROM %s WHERE playername = '%s';", PLAYER_TABLE, playername));
    }

    public void unbanIp(String ip) throws SQLException
    {
        execute(String.format("DELETE FROM %s WHERE ip = '%s';", IP_TABLE, ip));
    }
}
