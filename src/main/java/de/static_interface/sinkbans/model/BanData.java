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

package de.static_interface.sinkbans.model;

import org.bukkit.ChatColor;

public class BanData implements Comparable<BanData> {

    private boolean isBanned;

    private long banTimeStamp;
    private long unbanTimeStamp;

    private String reason;
    private String playername;
    private String ip;
    private String banner;
    private String uuid;
    private int id;
    private String unbanner;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }


    public BanData(String playername) {
        this.playername = playername;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        this.isBanned = banned;
    }

    public boolean isPermBan() {
        return unbanTimeStamp == -1;
    }

    public boolean isTempBanned() {
        return unbanTimeStamp > 0;
    }

    public String getReason() {
        return ChatColor.RED + reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getUnbanTimeStamp() {
        return unbanTimeStamp;
    }

    public void setUnbanTimeStamp(long unbanTimestamp) {
        this.unbanTimeStamp = unbanTimestamp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getName() {
        return playername;
    }

    public String getUniqueId() {
        return uuid;
    }

    public void setUniqueId(String uuid) {
        this.uuid = uuid;
    }

    public long getBanTimeStamp() {
        return banTimeStamp;
    }

    public void setBanTimeStamp(long banTimeStamp) {
        this.banTimeStamp = banTimeStamp;
    }

    @Override
    public int compareTo(BanData banData) {
        if (banData.getBanTimeStamp() > getBanTimeStamp()) {
            return -1;
        } else if (banData.getBanTimeStamp() < getBanTimeStamp()) {
            return 1;
        }
        return 0;
    }

    public String getUnbanner() {
        return unbanner;
    }

    public void setUnbanner(String unbanner) {
        this.unbanner = unbanner;
    }
}
