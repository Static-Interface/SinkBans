package de.static_interface.sinkbans.commands;

import de.static_interface.sinkbans.MySQLDatabase;
import de.static_interface.sinkbans.Util;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.sender.IrcCommandSender;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class BanCommand extends SinkCommand {

    private MySQLDatabase db;

    public BanCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        String reason = "";
        for (int i = 1; i < args.length; i++) {
            reason += " " + args[i];
        }

        if (args.length == 1 || reason.isEmpty()) {
            reason = "Du wurdest permanent gebannt.";
        }

        reason = ChatColor.RED + reason;

        IngameUser target = SinkLibrary.getInstance().getIngameUser(args[0], false);

        String targetName = target.isOnline() ? target.getName() : args[0];
        String prefix = BukkitUtil.getSenderName(sender);

        String reasonPrefix = ChatColor.DARK_RED + "Gesperrt: ";

        if (target.isOnline()) {
            target.getPlayer().kickPlayer(reasonPrefix + reason);
        }

        try {
            db.unban(target.getUniqueId(), target.getName(), sender.getName()); // Unban all bans done before
            db.ban(target.getName(), reason, sender.getName(), Util.getUniqueId(targetName, db), BanType.MANUAL_BAN);
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.DARK_RED + "Ein Fehler ist aufgetreten!");
            return true;
        }

        String msg = ChatColor.GOLD + prefix + ChatColor.GOLD + " hat " + ChatColor.RED + targetName + ChatColor.GOLD + " gesperrt:" + reason.trim();
        BukkitUtil.broadcast(msg, "sinkbans.notification", false);
        if (sender instanceof IrcCommandSender) {
            sender.sendMessage(msg);
        }

        return true;
    }
}
