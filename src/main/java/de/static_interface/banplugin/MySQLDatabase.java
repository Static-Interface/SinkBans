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
                "isbanned TINYINT(0) NOT NULL, " +
                "bannedby VARCHAR(16) NOT NULL, "+
                "unbannedby VARCHAR(16)" +
                ");", PLAYER_TABLE);
        execute(query);

        query = String.format("CREATE TABLE IF NOT EXISTS %s (" +
                "id INT NOT NULL AUTO_INCREMENT UNIQUE KEY, " +
                "ip VARCHAR(15) NOT NULL PRIMARY KEY NOT NULL, " +
                "reason TEXT CHARACTER SET utf8 NOT NULL, " +
                "bantimestamp BIGINT NOT NULL, " +
                "isbanned TINYINT(0) NOT NULL, " +
                "bannedby VARCHAR(16) NOT NULL, "+
                "unbannedby VARCHAR(16)" +
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
        ResultSet result = executeQuery(String.format("SELECT * FROM %s", PLAYER_TABLE)); // WHERE playername = '%s';", PLAYER_TABLE, playername));
        ArrayList<BanData> entries = new ArrayList<>();
        BanData data = getFromResultSet(result, playername, false);
        if (data != null)
            entries.add(data);
        while(result.next())
        {
            data = getFromResultSet(result, playername, false);
            if (data != null)
                entries.add(data);
        }
        result = executeQuery(String.format("SELECT * FROM %s", IP_TABLE)); // WHERE ip = '%s';", IP_TABLE, ip));
        data = getFromResultSet(result, playername, true);
        if (data != null)
            entries.add(data);

        while(result.next())
        {
            data = getFromResultSet(result, playername, true);
            if (data != null)
                entries.add(data);
        }

        return entries;
    }

    public BanData getBanData(String searchString, boolean isIp) throws SQLException
    {
        String query = isIp
                ? String.format("SELECT * FROM %s WHERE ip = '%s';", IP_TABLE, searchString)
                : String.format("SELECT * FROM %s WHERE playername = '%s';", PLAYER_TABLE, searchString);

        //Bukkit.getLogger().log(Level.INFO, "[Debug]: Executing Query:");
        //Bukkit.getLogger().log(Level.INFO, query);

        ResultSet result = executeQuery(query);
        return getFromResultSet(result, searchString, isIp);
    }

    public BanData getFromResultSet(ResultSet resultSet, String playername, boolean isIp)
    {
        try
        {
            if (resultSet.next())
            {
                BanData data = new BanData(playername);
                data.setBanner(resultSet.getString("bannedby"));
                data.setReason(resultSet.getString("reason"));
                data.setBanTimeStamp(resultSet.getLong("bantimestamp"));
                if (!isIp) data.setUnbanTimeStamp(resultSet.getLong("unbantimestamp"));
                if (isIp) data.setIp(resultSet.getString("ip"));
                data.setBanned(resultSet.getBoolean("isbanned"));
                return data;
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public void ban(String playername, String reason, String bannedby) throws SQLException
    {
        long banTimeStamp = System.currentTimeMillis();
        reason = reason.trim();
        reason = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', reason));
        try
        {
            execute(String.format("INSERT INTO %s VALUES(NULL, '%s','%s','%s', '-1', '1', '%s', NULL)", PLAYER_TABLE, playername, reason, banTimeStamp, bannedby));
        }
        catch(Exception e) //Already added
        {
            execute(String.format("UPDATE %s SET reason = '%s', bantimestamp = '%s', unbantimestamp = -1, isbanned = 1, bannedby = '%s', unbannedby = NULL WHERE playername = '%s'", PLAYER_TABLE, reason, banTimeStamp, bannedby, playername));
        }

    }

    public void tempBan(String playername, long unbanTimestamp, String bannedby) throws SQLException
    {

        String reason = "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(unbanTimestamp);
        long banTimeStamp = System.currentTimeMillis();
        try
        {
            execute(String.format("INSERT INTO %s VALUES(NULL, '%s', '%s', '%s', '%s', '1', '%s', NULL)", PLAYER_TABLE, playername, reason, banTimeStamp, unbanTimestamp, bannedby));
        }
        catch(Exception e) // Already added?
        {
            execute(String.format("UPDATE %s SET reason = '%s', bantimestamp = '%s', unbantimestamp = '%s', isbanned = 1, bannedby = '%s', unbannedby = NULL WHERE playername = '%s'", PLAYER_TABLE, reason, banTimeStamp, unbanTimestamp, bannedby, playername));
        }
    }

    public void banIp(String ip, String bannedby) throws SQLException
    {
        long banTimeStamp = System.currentTimeMillis();
        String reason = "";

        try
        {
            execute(String.format("INSERT INTO %s VALUES(NULL, '%s', '%s', '%s', '1', '%s', NULL)", IP_TABLE, ip, reason, banTimeStamp, bannedby));
        }
        catch(Exception e) //Already added?
        {
            execute(String.format("UPDATE %s SET reason = '%s', bantimestamp = '%s', isbanned = 1, bannedby = '%s', unbannedby = NULL WHERE ip = '%s'", IP_TABLE, reason, banTimeStamp, bannedby, ip));
        }
    }

    public void unban(String playername, String unbannedby) throws SQLException
    {
        long unbanTimeStamp = System.currentTimeMillis();
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = '%s', unbantimestamp = %s WHERE playername = '%s';" , PLAYER_TABLE, unbannedby, unbanTimeStamp, playername));
    }

    public void unbanTempBan(String playername) throws SQLException
    {
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = NULL WHERE playername = '%s';" , PLAYER_TABLE, playername));
    }

    public void unbanIp(String ip, String unbannedby) throws SQLException
    {
        execute(String.format("UPDATE %s SET isbanned = 0, unbannedby = '%s' WHERE ip = '%s';", IP_TABLE, unbannedby, ip));
    }
}
