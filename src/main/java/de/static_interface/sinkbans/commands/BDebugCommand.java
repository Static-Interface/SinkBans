/*
 * Copyright (c) 2013 - 2014 http://static-interface.de and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.sinkbans.commands;

import de.static_interface.sinkbans.MySQLDatabase;
import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.List;

public class BDebugCommand extends SinkCommand {

    private final MySQLDatabase db;

    public BDebugCommand(Plugin plugin, MySQLDatabase db) {
        super(plugin);
        this.db = db;
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        try {
            sender.sendMessage("Output: " + String.valueOf(handleMultiAccount(sender, args[0])));
        } catch (SQLException e) {
            sender.sendMessage(e.toString());
        }
        return true;
    }

    private boolean handleMultiAccount(CommandSender sender, String ip) throws SQLException {
        List<Account> accounts = db.getAccounts(ip);
        if (accounts.size() < 2) {
            return false;
        }
        boolean illegal = false;
        for (Account account : accounts) {
            if (!isAllowed(account)) {
                sender.sendMessage("isAllowed failed on " + account.getPlayername() + ":" + account.getUniqueId().toString());
                illegal = true;
                break;
            }
        }

        return illegal;
    }

    private boolean isAllowed(Account account) throws SQLException {
        return db.isAllowedMultiAccount(account);
    }
}
