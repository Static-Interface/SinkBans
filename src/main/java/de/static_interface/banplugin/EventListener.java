package de.static_interface.banplugin;

import de.static_interface.sinklibrary.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.SQLException;
import java.util.logging.Level;

public class EventListener implements Listener
{
    private MySQLDatabase database;
    public EventListener(MySQLDatabase database)
    {
        this.database = database;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        BanData data;

        //Todo: log every damn ip with uuid as key and playername
        String ip = Util.getIp(event.getAddress());

        try
        {
            data = database.getBanData(event.getUniqueId().toString(), false);
            if (isBanned(data, event))
            {
                Bukkit.getLogger().log(Level.INFO, "[DEBUG] Current Timestamp: " + System.currentTimeMillis() + ", Unban Timestamp: " + data.getUnbanTimestamp());
                Bukkit.getLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " is banned, disconnecting" );
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
            if (isBanned(data, event))
            {
                Bukkit.getLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " is IP banned, disconnecting" );
                BukkitUtil.broadcast(ChatColor.DARK_RED + "[BanPlugin] " + ChatColor.RED + "Warnung! Der IP gesperrte Spieler " + event.getName() + " mit der IP " + ip + " versuchte " +
                        "sich gerade einzuloggen!", "banplugin.notification:", false);
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

    }

    public boolean isBanned(BanData data, AsyncPlayerPreLoginEvent event)
    {
        if (data == null) return false;
        if (!data.isBanned()) return false;
        if (data.isPermaBanned())
        {
            String reason = data.getReason();
            if (reason == null || reason.isEmpty()) reason = "Deine IP wurde gesperrt.";
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
            return true;
        }
        else if (data.isTempBanned())
        {
            if ((data.getUnbanTimestamp() - System.currentTimeMillis()) <= 0)
            {
                try
                {
                    database.unbanTempBan(event.getName());
                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                }
                return false;
            }
            else
            {
                String reason = ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(data.getUnbanTimestamp());
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
                return true;
            }
        }
        return false;
    }
}
