package de.static_interface.banplugin;

import java.net.InetAddress;
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
}
