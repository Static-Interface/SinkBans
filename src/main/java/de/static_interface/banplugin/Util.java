package de.static_interface.banplugin;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.List;
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
}
