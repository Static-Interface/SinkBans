package de.static_interface.banplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.SQLException;
import java.util.ArrayList;
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
        ArrayList<BanData> data;
        try
        {
            data = database.getBanData(event.getName(), event.getAddress().getHostName().split(":")[0]);
        }
        catch ( SQLException e )
        {
            //Data doesn't exists
            return;
        }

        for(BanData player : data)
        {
            Bukkit.getLogger().log(Level.INFO, "DEBUG: Banned: " + player.getName() + ": " + player.getIp());
            if (player.isBanned())
            {
                String reason = player.getReason();
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
            }
            else if (player.isTempBanned())
            {
                if (System.currentTimeMillis() >= player.getUnbanTimestamp())
                {
                    try
                    {
                        database.unban(event.getName());
                    }
                    catch ( SQLException e )
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    String reason = ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil.formatDateDiff(player.getUnbanTimestamp());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
                }
            }
        }
    }
}
