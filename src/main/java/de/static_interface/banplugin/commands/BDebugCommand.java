package de.static_interface.banplugin.commands;

import de.static_interface.banplugin.Account;
import de.static_interface.banplugin.MySQLDatabase;
import de.static_interface.sinklibrary.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;

public class BDebugCommand extends Command
{
    private final MySQLDatabase db;

    public BDebugCommand(Plugin plugin, MySQLDatabase db)
    {
        super(plugin);
        this.db = db;
    }

    @Override
    public boolean isIrcOpOnly() { return true; }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args)
    {
        if(args.length < 1) return false;
        try
        {
            sender.sendMessage("Output: " + String.valueOf(handleMultiAccount(args[0])));
        }
        catch ( SQLException e )
        {
            sender.sendMessage(e.toString());
        }
        return true;
    }

    private boolean handleMultiAccount(String ip) throws SQLException
    {
        List<Account> accounts = db.getAccounts(ip);
        if(accounts.size() < 2) return false;
        boolean illegal = false;
        for(Account account : accounts)
        {
            if(!isAllowed(account))
            {
                sender.sendMessage("isAllowed failed on " +account.getPlayername() + ":" + account.getUniqueId().toString());
                illegal = true;
                break;
            }
        }

        return illegal;
    }

    private boolean isAllowed(Account account) throws SQLException
    {
        return db.isAllowedMultiAccount(account);
    }
}
