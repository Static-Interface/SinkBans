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

import de.static_interface.sinkbans.database.BanRequestsTable;
import de.static_interface.sinkbans.database.BanTable;
import de.static_interface.sinkbans.database.IpBanTable;
import de.static_interface.sinkbans.database.SessionsTable;
import de.static_interface.sinklibrary.database.DatabaseConfiguration;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinksql.Database;
import de.static_interface.sinksql.DatabaseConnectionInfo;
import de.static_interface.sinksql.impl.database.MySqlDatabase;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Util {
    public static final String IP_PATTERN = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
    public static final String IP_MASK_PATTERN = "/^\\d{1,3}(\\.\\d{1,3}){3}\\/\\d{1,2}$/";

    public static String getIp(InetAddress address) {
        return address.getHostAddress();
    }

    public static boolean isValidIp(String ip) {
        return (BuildFlags.ENABLE_IPTABLES && Pattern.matches(IP_MASK_PATTERN, ip)) || Pattern.matches(IP_PATTERN, ip);
    }

    public static void runCommandAsync(final String cmdline){
        Bukkit.getScheduler().runTaskAsynchronously(SinkBans.getInstance(), new Runnable() {
            @Override
            public void run() {
                try {
                    Process proc = Runtime.getRuntime().exec(cmdline);

                    InputStream stdin = proc.getInputStream();
                    InputStreamReader isr = new InputStreamReader(stdin);
                    BufferedReader br = new BufferedReader(isr);

                    String line;
                    SinkBans.getInstance().getLogger().info("> " + cmdline);

                    while ( (line = br.readLine()) != null)
                        System.out.println(line);

                    if(Debug.isEnabled()) {
                        int exitVal = proc.waitFor();
                        Debug.log("Process ExitCode: " + exitVal);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Work-Around for a class loading bug
    static void loadDatabase(SinkBans instance){
        DatabaseConnectionInfo info = new DatabaseConfiguration(instance.getDataFolder(), "sinkbans", instance);
        Database db = new MySqlDatabase(info);
        instance.setDb(db);
        BanTable banTable = new BanTable(db);
        instance.setBanTable(banTable);
        SessionsTable sessionsTable = new SessionsTable(db);
        instance.setSessionsTable(sessionsTable);
        BanRequestsTable banRequestsTable = new BanRequestsTable(db);
        instance.setBanRequestsTable(banRequestsTable);
        IpBanTable ipBans = new IpBanTable(db);
        instance.setIpBansTable(ipBans);
        try {
            db.connect();
            banTable.create();
            ipBans.create();
            sessionsTable.create();
            banRequestsTable.create();
        } catch (SQLException e) {
            instance.getLogger().severe("FAILED TO CREATE REQUIRED TABLES. SHUTDOWN...");
            e.printStackTrace();
            Bukkit.shutdown();
        }
    }
}
