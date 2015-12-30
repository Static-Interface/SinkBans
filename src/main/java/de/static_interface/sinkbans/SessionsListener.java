/*
 * Copyright (c) 2013 - 2015 http://static-interface.de and contributors
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

import de.static_interface.sinkbans.database.Session;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SessionsListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerPreLoginLowest(AsyncPlayerPreLoginEvent event){
        SessionManager.startSession(event.getUniqueId(), event.getName(), event.getAddress());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerPreLoginMonitor(AsyncPlayerPreLoginEvent event){
        if(event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            Session session = SessionManager.getActiveSession(Bukkit.getOfflinePlayer(event.getUniqueId()));
            SessionManager.setProperty(session, "kicked", true);
            SessionManager.setProperty(session, "disconnectReason", event.getKickMessage());
            SessionManager.stopSession(Bukkit.getOfflinePlayer(event.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId());
        if(!SessionManager.hasActiveSession(player)) return;
        Session session = SessionManager.getActiveSession(player);
        SessionManager.setProperty(session, "disconnectReason", "disconnect");
        SessionManager.stopSession(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        Session session = SessionManager.getActiveSession(Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId()));
        SessionManager.setProperty(session, "disconnectReason", event.getReason());
        SessionManager.setProperty(session, "kicked", true);
        SessionManager.stopSession(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAuthLogin(LoginEvent event) {
        if(!SessionManager.hasActiveSession(event.getPlayer())) {
            SessionManager.startSession(event.getPlayer().getUniqueId(), event.getPlayer().getName(), event.getPlayer().getAddress().getAddress());
        }
        Session session = SessionManager.getActiveSession(event.getPlayer());
        SessionManager.setProperty(session, "authed", true);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAuthLogout(LogoutEvent event) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId());
        Session session = SessionManager.getActiveSession(player);
        SessionManager.setProperty(session, "disconnectReason", "auth_logout");
        SessionManager.stopSession(event.getPlayer());
    }
}
