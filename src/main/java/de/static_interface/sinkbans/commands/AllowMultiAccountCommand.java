package de.static_interface.sinkbans.commands;

import de.static_interface.sinkbans.MySQLDatabase;
import de.static_interface.sinkbans.Util;
import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.UUID;

public class AllowMultiAccountCommand extends SinkCommand {

    private final MySQLDatabase db;

    public AllowMultiAccountCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        String name = SinkLibrary.getInstance().getIngameUser(args[0]).getName();
        UUID uuid = Util.getUniqueId(name, db);

        try {
            Account acc = new Account(uuid, name);
            if (db.isAllowedMultiAccount(acc)) {
                sender.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + " Dieser Account ist bereits auf der Whitelist!");
                return true;
            }
            db.addMultiAccount(acc);
            sender.sendMessage(ChatColor.DARK_GREEN + name + " wurde erfolgreich zur MultiAccount Whitelist hinzugefügt!");
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Ein Fehler ist aufgetreten.");
        }
        return true;
    }
}
