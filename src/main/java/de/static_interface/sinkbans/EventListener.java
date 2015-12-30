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

import de.static_interface.sinkbans.database.BanRequest;
import de.static_interface.sinkbans.database.DatabaseBanManager;
import de.static_interface.sinkbans.database.IpBanRow;
import de.static_interface.sinkbans.model.Account;
import de.static_interface.sinkbans.model.RequestState;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class EventListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = Util.getIp(event.getAddress());

        List<IpBanRow> data = DatabaseBanManager.getUserBans(ip);
        if (handleIpBan(data, event, true)) {
            SinkBans.getInstance().getLogger().log(Level.INFO, "[Ban] Player " + event.getName() + " is IP banned, disconnecting");
            BukkitUtil.broadcast(
                    ChatColor.DARK_RED + "[SinkBans] " + ChatColor.RED + "Warnung! Der IP gesperrte Spieler " + event.getName() + " mit der IP "
                    + ip + " versuchte " +
                    "sich gerade einzuloggen!", "sinkbans.notification:");
            return;
        }

        /*
        try {
            List<Account> accounts = DbUtil.getAccounts(ip);
            String accountMessage = getAccountMessage(accounts, event.getName());
            if (accountMessage != null && handleMultiAccount(ip, event)) {
                BukkitUtil.broadcast(ChatColor.DARK_RED + "[SinkBans] " + ChatColor.RED + "Warnung! " + event.getName()
                                     + " ist ein nicht freigeschalteter MultiAccount und versuche sich einzuloggen!", "sinkbans.notification");
                BukkitUtil.broadcast(ChatColor.RED + "Weitere Accounts: " + ChatColor.RESET + accountMessage, "sinkbans.notification");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        */
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(!event.getPlayer().hasPermission("sinkbans.denybanrequest") && !event.getPlayer().hasPermission("sinkbans.acceptbanrequest")) {
            return;
        }
        if(DatabaseBanManager.getAllBanRequests().size() == 0) {
            return;
        }

        int amount = 0;
        for(BanRequest data : DatabaseBanManager.getAllBanRequests()) {
            if(data.state == RequestState.PENDING) {
                amount++;
            }
        }
        if(amount == 0) return;
        event.getPlayer().sendMessage(ChatColor.DARK_RED + "Es sind " + ChatColor.RED + amount + ChatColor.DARK_RED + " Bananfragen offen!");
    }

    /*
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
    */

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

    public boolean handleIpBan(List<IpBanRow> datas, AsyncPlayerPreLoginEvent event, boolean isIp) {
        if (datas == null) {
            return false;
        }
        for (IpBanRow data : datas) {
            if(data == null) continue;
            if (!data.isbanned) {
                continue;
            }
            if (data.unbantimestamp < 0) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + "Deine IP ist gesperrt");
            } else {
                if ((data.unbantimestamp - System.currentTimeMillis()) <= 0) {
                    DatabaseBanManager.unbanTempBan(event.getUniqueId(), null, data.unbantimestamp);
                } else {
                    String
                            reason =
                            ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RED + "Zeitlich gesperrt vom Server fuer " + DateUtil
                                    .formatDateDiff(data.unbantimestamp);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.DARK_RED + "Gesperrt: " + ChatColor.RESET + reason);
                }
            }

            IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getUniqueId());
            if (isIp && !user.isBanned()) {
                user.ban("Deien IP wurde gesperrt");
            }
            return true;
        }
        return false;
    }
}
