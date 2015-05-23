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

import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinkbans.model.BanData;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class EventListener implements Listener {

    private MySQLDatabase database;

    public EventListener(MySQLDatabase database) {
        this.database = database;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        List<BanData> data;
        String ip = Util.getIp(event.getAddress());
        try {
            database.logIp(event.getUniqueId(), event.getName(), ip);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            List<BanData> oldData = database.getOldBanData(event.getName());
            data = database.getBanData(event.getUniqueId().toString(), false);
            if (handleData(data, event, false) || handleData(oldData, event, false)) {
                SinkLibrary.getInstance().getCustomLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " is banned, disconnecting");
                BukkitUtil.broadcast(
                        ChatColor.DARK_RED + "[SinkBans] " + ChatColor.RED + "Warnung! Der gesperrte Spieler " + event.getName() + " versuchte " +
                        "sich gerade einzuloggen!", "sinkbans.notification:", false);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        try {
            data = database.getBanData(ip, true);
            if (handleData(data, event, true)) {
                SinkLibrary.getInstance().getCustomLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " is IP banned, disconnecting");
                BukkitUtil.broadcast(
                        ChatColor.DARK_RED + "[SinkBans] " + ChatColor.RED + "Warnung! Der IP gesperrte Spieler " + event.getName() + " mit der IP "
                        + ip + " versuchte " +
                        "sich gerade einzuloggen!", "sinkbans.notification:", false);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            List<Account> accounts = database.getAccounts(ip);
            String accountMessage = getAccountMessage(accounts, event.getName());
            if (accountMessage != null && handleMultiAccount(ip, event)) {
                BukkitUtil.broadcast(ChatColor.DARK_RED + "[SinkBans] " + ChatColor.RED + "Warnung! " + event.getName()
                                     + " ist ein nicht freigeschalteter MultiAccount und versuche sich einzuloggen!", "sinkbans.notification",
                                     false);
                BukkitUtil.broadcast(ChatColor.RED + "Weitere Accounts: " + ChatColor.RESET + accountMessage, "sinkbans.notification", false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean handleMultiAccount(String ip, AsyncPlayerPreLoginEvent event) throws SQLException {
        List<Account> accounts = database.getAccounts(ip);
        if (accounts.size() < 2) {
            return false;
        }
        boolean illegal = false;
        for (Account account : accounts) {
            if (!isAllowed(account, ip)) {
                illegal = true;
                break;
            }
        }

        if (!illegal) {
            return false;
        }

        for (Account account : accounts) {
            if (Util.isBanned(account, database)) {
                continue;
            }
            database.ban(account.getPlayername(), null, null, account.getUniqueId(), BanType.AUTO_MULTI_ACC);
        }

        String accountMessage = getAccountMessage(accounts, event.getName());
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                       "Unangemeldeter MultiAccount. Bitte melde dich bei einem Minister oder Administrator." + (accountMessage != null ? " (Accounts: " + accountMessage  + ")" : ""));
        return true;
    }

    private String getAccountMessage(List<Account> accounts, @Nullable String ignoredName) {
        String accountMessage = null;
        for (Account acc : accounts) {
            if (ignoredName != null && acc.getPlayername().equalsIgnoreCase(ignoredName)) {
                continue;
            }
            if (accountMessage == null) {
                continue;
            }
            accountMessage += ", " + acc.getPlayername();
        }

        return accountMessage;
    }

    private boolean isAllowed(Account account, String ip) {
        try {
            if(database.countIps(ip, 14 * 24) > 2) {
                return database.isAllowedMultiAccount(account);
            }
            else if(database.countIps(ip, 14 * 24) > 1 && database.getFirstJoin(account, ip) >= System.currentTimeMillis() - 2 * 60 * 60 * 1000 && database.getFirstJoin(account, ip) <= System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
                return database.isAllowedMultiAccount(account);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleData(List<BanData> datas, AsyncPlayerPreLoginEvent event, boolean isIp) {
        if (datas == null) {
            return false;
        }
        for (BanData data : datas) {
            if (!isIp && data.getUniqueId() == null || data.getUniqueId().isEmpty()) {
                try {
                    database.fixBan(data, event.getUniqueId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (!data.isBanned()) {
                continue;
            }
            if (data.isPermBan()) {
                String reason = data.getReason();
                if (reason == null || reason.isEmpty()) {
                    reason = "Du wurdest permanent gesperrt.";
                }
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
            } else if (data.isTempBanned()) {
                if ((data.getUnbanTimeStamp() - System.currentTimeMillis()) <= 0) {
                    try {
                        database.unbanTempBan(event.getUniqueId(), event.getName(), data.getUnbanTimeStamp());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    String
                            reason =
                            ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil
                                    .formatDateDiff(data.getUnbanTimeStamp());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
                }
            }
            if (isIp && !Util.isBanned(new Account(event.getUniqueId(), event.getName()), database)) {
                try {
                    database.ban(event.getName(), null, data.getBanner(), event.getUniqueId(), BanType.AUTO_IP);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }
}
