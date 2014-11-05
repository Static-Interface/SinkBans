package de.static_interface.sinkbans.commands;

import de.static_interface.sinkbans.MySQLDatabase;
import de.static_interface.sinkbans.Util;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.sender.IrcCommandSender;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class UnbanCommand extends SinkCommand {

    private MySQLDatabase db;

    public UnbanCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        String targetName = args[0];
        IngameUser target = SinkLibrary.getInstance().getIngameUser(args[0], false);
        String prefix = BukkitUtil.getSenderName(sender);

        try {
            db.unban(Util.getUniqueId(target.getName(), db), target.getName(), sender.getName());
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
            return true;
        }
        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat " + ChatColor.RED + targetName + ChatColor.GOLD + " entsperrt.";
        BukkitUtil.broadcast(msg, "sinkbans.notification", false);
        if (sender instanceof IrcCommandSender) {
            sender.sendMessage(msg);
        }
        return true;
    }
}
