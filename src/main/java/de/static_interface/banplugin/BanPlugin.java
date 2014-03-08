package de.static_interface.banplugin;

import de.static_interface.banplugin.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BanPlugin extends JavaPlugin
{
    MySQLDatabase db;
    public void onEnable()
    {
        try
        {
            db = new MySQLDatabase("localhost", "3306", "banplugin", "root", "");
        }
        catch ( Exception e )
        {
            throw new RuntimeException(e);
        }
        registerCommands();
        Bukkit.getPluginManager().registerEvents(new EventListener(db), this);
    }

    private void registerCommands()
    {
        Bukkit.getPluginCommand("ban").setExecutor(new BanCommand(db));
        Bukkit.getPluginCommand("banip").setExecutor(new BanIpCommand(db));
        Bukkit.getPluginCommand("isbanned").setExecutor(new IsBannedCommand(db));
        Bukkit.getPluginCommand("tempban").setExecutor(new TempBanCommand(db));
        Bukkit.getPluginCommand("unban").setExecutor(new UnbanCommand(db));
        Bukkit.getPluginCommand("unbanip").setExecutor(new UnbanIpCommand(db));
    }
}
