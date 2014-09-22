package de.static_interface.banplugin;

import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class EventListener implements Listener
{
    private MySQLDatabase database;
    public EventListener(MySQLDatabase database)
    {
        this.database = database;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        List<BanData> data;
        String ip = Util.getIp(event.getAddress());
        try
        {
            database.logIp(event.getUniqueId(), event.getName(), ip);
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        try
        {
            List<BanData> oldData = database.getOldBanData(event.getName());
            data = database.getBanData(event.getUniqueId().toString(), false);
            if ( handleData(data, event, false) || handleData(oldData, event, false))
            {
                SinkLibrary.getCustomLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " is banned, disconnecting");
                BukkitUtil.broadcast(ChatColor.DARK_RED + "[BanPlugin] " + ChatColor.RED + "Warnung! Der gesperrte Spieler " + event.getName() + " versuchte " +
                        "sich gerade einzuloggen!", "banplugin.notification:", false);
                return;
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return;
        }

        try
        {
            data = database.getBanData(ip, true);
            if ( handleData(data, event, true))
            {
                SinkLibrary.getCustomLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " is IP banned, disconnecting" );
                BukkitUtil.broadcast(ChatColor.DARK_RED + "[BanPlugin] " + ChatColor.RED + "Warnung! Der IP gesperrte Spieler " + event.getName() + " mit der IP " + ip + " versuchte " +
                        "sich gerade einzuloggen!", "banplugin.notification:", false);
                return;
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }


        try
        {
            if( handleMultiAccount(ip, event))
            {
                SinkLibrary.getCustomLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " got banned for MulitAccounts, disconnecting");
                BukkitUtil.broadcast(ChatColor.DARK_RED + "[BanPlugin] " + ChatColor.RED + "Warnung! " + event.getName() + " ist ein nicht freigeschalteter MultiAccount und versuche sich einzuloggen!", "banplugin.notification", false);
                List<Account> accounts = database.getAccounts(ip);
                String msg = null;
                for(Account acc : accounts)
                {
                    if(msg == null)
                    {
                        msg = acc.getPlayername();
                        continue;
                    }
                    msg += ", " + acc.getPlayername();
                }
                BukkitUtil.broadcast(ChatColor.RED + "Weitere Accounts: " + ChatColor.RESET + msg, "banplugin.notification", false);
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }
    }

    private boolean handleMultiAccount(String ip, AsyncPlayerPreLoginEvent event) throws SQLException
    {
        List<Account> accounts = database.getAccounts(ip);
        if(accounts.size() < 2) return false;
        boolean illegal = false;
        for(Account account : accounts)
        {
            if(!isAllowed(account))
            {
                illegal = true;
                break;
            }
        }

        if(!illegal) return false;

        for(Account account : accounts)
        {
            if(Util.isBanned(account, database)) continue;
            database.ban(account.getPlayername(), null, null, account.getUniqueId(), BanType.AUTO_MULTI_ACC);
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "Unangemeldeter MultiAccount. Bitte melde dich bei einem Moderator oder Administrator.");
        return true;
    }

    private boolean isAllowed(Account account)
    {
        try
        {
            return database.isAllowedMultiAccount(account);
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleData(List<BanData> datas, AsyncPlayerPreLoginEvent event, boolean isIp)
    {
        if(datas == null) return false;
        for(BanData data : datas)
        {
            if(!isIp && data.getUniqueId() == null || data.getUniqueId().isEmpty())
            {
                try
                {
                    database.fixBan(data, event.getUniqueId());
                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                }
            }
            if ( !data.isBanned() ) continue;
            if ( data.isPermBan() )
            {
                String reason = data.getReason();
                if ( reason == null || reason.isEmpty() ) reason = "Du wurdest permanent gesperrt.";
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
            }
            else if ( data.isTempBanned() )
            {
                if ( (data.getUnbanTimeStamp() - System.currentTimeMillis()) <= 0 )
                {
                    try
                    {
                        database.unbanTempBan(event.getUniqueId(), event.getName(), data.getUnbanTimeStamp());
                    }
                    catch ( SQLException e )
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    String reason = ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(data.getUnbanTimeStamp());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
                }
            }
            if(isIp && !Util.isBanned(new Account(event.getUniqueId(), event.getName()), database))
            {
                try
                {
                    database.ban(event.getName(), null, data.getBanner(), event.getUniqueId(), BanType.AUTO_IP);
                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }
}
