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

import de.static_interface.sinkbans.commands.BanCommand;
import de.static_interface.sinkbans.commands.BanIpCommand;
import de.static_interface.sinkbans.commands.BanRequestCommand;
import de.static_interface.sinkbans.commands.IsBannedCommand;
import de.static_interface.sinkbans.commands.KickCommand;
import de.static_interface.sinkbans.commands.TempBanCommand;
import de.static_interface.sinkbans.commands.UnbanCommand;
import de.static_interface.sinkbans.commands.UnbanIpCommand;
import de.static_interface.sinkbans.database.BanRequestsTable;
import de.static_interface.sinkbans.database.BanTable;
import de.static_interface.sinkbans.database.IpBanTable;
import de.static_interface.sinkbans.database.SessionsTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.stream.BukkitBroadcastStream;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinksql.Database;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class SinkBans extends JavaPlugin {

    private static SinkBans instance;
    private Database db;
    private BanTable banTable;
    private SessionsTable sessionsTable;
    private BanRequestsTable banRequestsTable;
    private IpBanTable ipBans;

    public void onEnable() {
        if(!checkDependencies()) return;
        instance = this;
        SinkLibrary.getInstance().registerMessageStream(new BukkitBroadcastStream("sb_bans"));
        SinkLibrary.getInstance().registerMessageStream(new BukkitBroadcastStream("sb_bans_ip"));
        SinkLibrary.getInstance().registerMessageStream(new BukkitBroadcastStream("sb_bans_temp"));
        SinkLibrary.getInstance().registerMessageStream(new BukkitBroadcastStream("sb_ban_requests"));
        SinkLibrary.getInstance().registerMessageStream(new BukkitBroadcastStream("sb_kicks"));

        SinkLibrary.getInstance().registerBanProvider(new SinkBansBanProvider());

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new EventListener(), this);
        Bukkit.getPluginManager().registerEvents(new SessionsListener(), this);

        Util.loadDatabase(this);

        // In case of reload
        for(Player p : Bukkit.getOnlinePlayers()){
            SessionManager.startSession(p.getUniqueId(), p.getName(), p.getAddress().getAddress());
        }
    }

    public void onDisable() {
        for(Player p : Bukkit.getOnlinePlayers()){
            SessionManager.stopSession(p);
        }

        try {
            db.close();
        } catch (SQLException e) {
            Debug.log(Level.WARNING, "Unexpected exception: ", e);
        }
        instance = null;
    }

    public static SinkBans getInstance() {
        return instance;
    }

    public BanTable getBanTable(){
        return banTable;
    }

    public SessionsTable getSessionsTable() {
        return sessionsTable;
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
        SinkLibrary.getInstance().registerCommand("ban", new BanCommand(this));
        SinkLibrary.getInstance().registerCommand("banip", new BanIpCommand(this));
        SinkLibrary.getInstance().registerCommand("isbanned", new IsBannedCommand(this));
        SinkLibrary.getInstance().registerCommand("tempban", new TempBanCommand(this));
        SinkLibrary.getInstance().registerCommand("unban", new UnbanCommand(this));
        SinkLibrary.getInstance().registerCommand("unbanip", new UnbanIpCommand(this));
        //SinkLibrary.getInstance().registerCommand("allowmultiaccount", new AllowMultiAccountCommand(this, db));
        SinkLibrary.getInstance().registerCommand("banrequest", new BanRequestCommand(this));
        SinkLibrary.getInstance().registerCommand("kick", new KickCommand(this));
    }

    public BanRequestsTable getBanRequestsTable() {
        return banRequestsTable;
    }

    public IpBanTable getIpBanTable() {
        return ipBans;
    }

    void setDb(Database db) {
        this.db = db;
    }

    void setBanTable(BanTable banTable) {
        this.banTable = banTable;
    }

    void setSessionsTable(SessionsTable sessionsTable) {
        this.sessionsTable = sessionsTable;
    }

    void setBanRequestsTable(BanRequestsTable banRequestsTable) {
        this.banRequestsTable = banRequestsTable;
    }

    void setIpBansTable(IpBanTable ipBans) {
        this.ipBans = ipBans;
    }
}
