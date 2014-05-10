package de.static_interface.banplugin;

import java.net.InetAddress;

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
}
