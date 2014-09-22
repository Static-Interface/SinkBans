package de.static_interface.banplugin;

import de.static_interface.sinklibrary.BukkitUtil;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class Util
{
    public static String getIp(InetAddress address)
    {
        return address.getHostAddress();
    }

    public static final String IP_PATTERN = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
    public static boolean isValidIp(String ip)
    {
        return Pattern.matches(IP_PATTERN, ip);
    }

    public static boolean isBanned(Account acc, MySQLDatabase db)
    {
        try
        {
            List<BanData> datas = db.getBanData(acc.getUniqueId().toString(), false);
            datas.addAll(db.getOldBanData(acc.getPlayername()));
            for(BanData data : datas)
            {
                if(data.isBanned()) return true;
            }
            return false;
        }
        catch ( SQLException e )
        {
            throw new RuntimeException(e);
        }
    }

    public static UUID getUniqueId(String playername, MySQLDatabase database)
    {
        UUID uuid = null;
        try
        {
            ResultSet result = database.executeQuery(String.format(
                    "SELECT * FROM %s WHERE playername=?",
                    MySQLDatabase.LOG_TABLE), playername);
            if(result.next())
            {
                uuid = UUID.fromString(result.getString("uuid"));
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        if(uuid == null)
        {
            uuid = BukkitUtil.getUUIDByName(playername);
        }
        return uuid;
    }
}
