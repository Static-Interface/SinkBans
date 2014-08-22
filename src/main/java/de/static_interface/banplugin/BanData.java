package de.static_interface.banplugin;

import org.bukkit.ChatColor;

public class BanData implements Comparable<BanData>
{

    private boolean isBanned;

    private long banTimeStamp;
    private long unbanTimeStamp;

    private String reason;
    private String playername;
    private String ip;
    private String banner;
    private String uuid;


    public BanData(String playername)
    {
        this.playername = playername;
    }

    public boolean isBanned()
    {
        return isBanned;
    }

    public boolean isPermBan()
    {
        return unbanTimeStamp == -1;
    }

    public boolean isTempBanned()
    {
        return unbanTimeStamp > 0;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getReason()
    {
        return ChatColor.RED + reason;
    }

    public long getUnbanTimeStamp()
    {
        return unbanTimeStamp;
    }

    public void setBanTimeStamp(long banTimeStamp)
    {
        this.banTimeStamp = banTimeStamp;
    }

    public void setUnbanTimeStamp(long unbanTimestamp)
    {
        this.unbanTimeStamp = unbanTimestamp;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public String getIp()
    {
        return ip;
    }

    public void setBanned(boolean banned)
    {
        this.isBanned = banned;
    }

    public String getBanner()
    {
        return banner;
    }

    public void setBanner(String banner)
    {
        this.banner = banner;
    }

    public String getName()
    {
        return playername;
    }

    public void setUniqueId(String uuid)
    {
        this.uuid = uuid;
    }

    public String getUniqueId()
    {
        return uuid;
    }

    public long getBanTimeStamp()
    {
        return banTimeStamp;
    }

    @Override
    public int compareTo(BanData banData)
    {
        if(banData.getBanTimeStamp() > getBanTimeStamp())
            return -1;
        else if(banData.getBanTimeStamp() < getBanTimeStamp())
            return 1;
        return 0;
    }
}
