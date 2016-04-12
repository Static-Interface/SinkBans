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

import de.static_interface.sinklibrary.util.Debug;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
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
}
