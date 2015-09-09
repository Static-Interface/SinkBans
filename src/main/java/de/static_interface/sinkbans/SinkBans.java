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

package de.static_interface.sinkbans;

import de.static_interface.sinkbans.commands.AllowMultiAccountCommand;
import de.static_interface.sinkbans.commands.BDebugCommand;
import de.static_interface.sinkbans.commands.BanCommand;
import de.static_interface.sinkbans.commands.BanIpCommand;
import de.static_interface.sinkbans.commands.BanRequestCommand;
import de.static_interface.sinkbans.commands.IsBannedCommand;
import de.static_interface.sinkbans.commands.TempBanCommand;
import de.static_interface.sinkbans.commands.UnbanCommand;
import de.static_interface.sinkbans.commands.UnbanIpCommand;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class SinkBans extends JavaPlugin {

    MySQLDatabase db;

    public void onEnable() {
        if(!checkDependencies()) return;

        try {
            db = new MySQLDatabase("localhost", "3306", "sinkbans", "root", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SinkLibrary.getInstance().registerBanProvider(new SinkBansBanProvider(db));

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new EventListener(db), this);
        try {
            db.fixOldBans();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("SinkLibrary") == null) {
            getLogger().log(Level.WARNING, "This Plugin requires SinkLibrary!");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        return SinkLibrary.getInstance().validateApiVersion(SinkLibrary.API_VERSION, this);
    }

    private void registerCommands() {
        SinkLibrary.getInstance().registerCommand("ban", new BanCommand(this, db));
        SinkLibrary.getInstance().registerCommand("banip", new BanIpCommand(this, db));
        SinkLibrary.getInstance().registerCommand("isbanned", new IsBannedCommand(this, db));
        SinkLibrary.getInstance().registerCommand("tempban", new TempBanCommand(this, db));
        SinkLibrary.getInstance().registerCommand("unban", new UnbanCommand(this, db));
        SinkLibrary.getInstance().registerCommand("unbanip", new UnbanIpCommand(this, db));
        SinkLibrary.getInstance().registerCommand("allowmultiaccount", new AllowMultiAccountCommand(this, db));
        SinkLibrary.getInstance().registerCommand("bdebug", new BDebugCommand(this, db));
        SinkLibrary.getInstance().registerCommand("banrequest", new BanRequestCommand(this, db));
    }
}
