package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.Account;
import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.sinklibrary.BukkitUtil;
import de.static_interface.sinklibrary.commands.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.UUID;

public class AllowMultiAccountCommand extends Command
{
    private final MySQLDatabase db;
    public AllowMultiAccountCommand(Plugin plugin, MySQLDatabase db)
    {
        super(plugin);
        this.db = db;
    }

    @Override
    public boolean isIrcOpOnly()
    {
        return true;
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args)
    {
        if(args.length < 2)
        {
            return false;
        }

        String name = args[0];
        UUID uuid = BukkitUtil.getUUIDByName(name);

        try
        {
            db.addMultiAccount(new Account(uuid, name));
            sender.sendMessage(ChatColor.DARK_GREEN + name + " wurde erfolgreich zur MultiAccount Whitelist hinzugefügt!");
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Ein Fehler ist aufgetreten.");
        }
        return true;
    }
}
