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

import static de.static_interface.sinksql.query.Query.eq;
import static de.static_interface.sinksql.query.Query.from;

import de.static_interface.sinkbans.database.Session;
import de.static_interface.sinklibrary.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SessionManager {
    private static Map<UUID, Session> activeSessions = new HashMap<>();

    public static Session startSession(UUID uuid, String name, InetAddress address){
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if(hasActiveSession(player)) {
            //Stop a session which already exists
            stopSession(player);
            Debug.log(Level.WARNING, "startSession() called before last session has ended (player: " + player.getName()  + ")");
        }

        Session session = new Session();
        session.ip = address.getHostAddress();
        session.startTimestamp =  System.currentTimeMillis();
        session.playerName = name;
        session.uuid = uuid.toString();
        session = SinkBans.getInstance().getSessionsTable().insert(session);
        activeSessions.put(uuid, session);
        return session;
    }

    public static void stopSession(OfflinePlayer player) {
        if(!hasActiveSession(player)) {
            if (Debug.isEnabled()) {
                throw new IllegalStateException("Can't stop session: Player " + player.getName() + " doesn't have any active sessions");
            }
            return;
        }

        Session session = activeSessions.get(player.getUniqueId());

        from(SinkBans.getInstance().getSessionsTable())
                .update()
                .set("stopTimestamp", "?")
                .where("id", eq("?"))
                .execute(System.currentTimeMillis(), session.id);

        activeSessions.remove(player.getUniqueId());
    }

    public static boolean hasActiveSession(OfflinePlayer player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public static Session getActiveSession(OfflinePlayer player) {
        if(!hasActiveSession(player)) return null;
        return activeSessions.get(player.getUniqueId());
    }

    public static void setProperty(Session session, String key, Object value) {
        from(SinkBans.getInstance().getSessionsTable())
                .update()
                .set(key, "?")
                .where("id", eq("?"))
                .execute(value, session.id);
    }
}
