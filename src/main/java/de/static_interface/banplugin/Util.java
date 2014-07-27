package de.static_interface.banplugin;

import java.net.InetAddress;
import java.util.regex.Pattern;

public class Util
{
    public static String getIp(InetAddress address)
    {
        String ip;
        try
        {
            ip = address.getHostName().split("/")[1].split(":")[0];
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            ip = address.getHostName().split(":")[0];
        }
        return ip;
    }

    public static final String IP_PATTERN = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
    public static boolean isValidIp(String ip)
    {
        return Pattern.matches(IP_PATTERN, ip);
    }
}
