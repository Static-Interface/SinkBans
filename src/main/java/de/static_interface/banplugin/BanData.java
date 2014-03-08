package de.static_interface.banplugin;

public class BanData
{

    private boolean isBanned;

    private long banTimestamp;
    private long unbanTimestamp;

    private String reason;
    private String playername;
    private String unbanTimeStamp;
    private String ip;
    private String banner;

    public BanData(String playername)
    {
        this.playername = playername;
    }

    public boolean isBanned()
    {
        return isBanned;
    }

    public boolean isTempBanned()
    {
        return unbanTimestamp > 0;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }

    public long getUnbanTimestamp()
    {
        return unbanTimestamp;
    }

    public void setBanTimeStamp(String banTimeStamp)
    {
        this.reason = banTimeStamp;
    }

    public void setUnbanTimeStamp(String unbanTimeStamp)
    {
        this.unbanTimeStamp = unbanTimeStamp;
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

    public Object getName()
    {
        return playername;
    }
}
