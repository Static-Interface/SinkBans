package de.static_interface.banplugin;

import de.static_interface.banplugin.commands.*;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

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
        try
        {
            db.fixOldBans();
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }
    }

    private void registerCommands()
    {
        SinkLibrary.registerCommand("ban", new BanCommand(this, db));
        SinkLibrary.registerCommand("banip", new BanIpCommand(this, db));
        SinkLibrary.registerCommand("isbanned", new IsBannedCommand(this, db));
        SinkLibrary.registerCommand("tempban", new TempBanCommand(this, db));
        SinkLibrary.registerCommand("unban", new UnbanCommand(this, db));
        SinkLibrary.registerCommand("unbanip", new UnbanIpCommand(this, db));
        SinkLibrary.registerCommand("allowmultiaccount", new AllowMultiAccountCommand(this, db));
        SinkLibrary.registerCommand("bdebug", new BDebugCommand(this, db));
    }
}
