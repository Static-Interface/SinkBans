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

import de.static_interface.sinkbans.model.BanData;
import de.static_interface.sinkbans.model.BanType;
import de.static_interface.sinklibrary.api.provider.BanProvider;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class SinkBansBanProvider implements BanProvider {

    private MySQLDatabase db;
    public SinkBansBanProvider(MySQLDatabase db) {
        this.db = db;
    }

    @Override
    public void ban(IngameUser user) {
        ban(user, null, null, null);
    }

    @Override
    public void ban(IngameUser user, @Nullable SinkUser banner) {
        ban(user, banner, null, null);
    }

    @Override
    public void ban(IngameUser user, @Nullable SinkUser banner, @Nullable String reason) {
        ban(user, banner, reason, null);
    }

    @Override
    public void ban(IngameUser user, @Nullable String reason) {
        ban(user, null, reason, null);
    }

    @Override
    public void ban(IngameUser user, @Nullable SinkUser banner, @Nullable Long timeOut) {
        ban(user, banner, null, timeOut);
    }

    @Override
    public void ban(IngameUser user, @Nullable Long timeOut) {
        ban(user, null, null, timeOut);
    }

    @Override
    public void ban(IngameUser user, @Nullable String reason, @Nullable Long timeOut) {
        ban(user, null, reason, timeOut);
    }


    @Override
    public void ban(IngameUser user, @Nullable SinkUser banner, @Nullable String reason, @Nullable Long timeOut) {
        int type = BanType.AUTO_BAN_API;
        String bannerName = banner == null ? null : banner.getName();

        if(banner != null) type = BanType.MANUAL_BAN;

        try {
            if(timeOut != null) {
                db.unbanTempBan(Util.getUniqueId(user.getName(), db), user.getName(), System.currentTimeMillis()); // Unban all bans done before
                db.tempBan(user.getName(), timeOut, bannerName, Util.getUniqueId(user.getName(), db), type);
            }
             else {
                db.unban(Util.getUniqueId(user.getName(), db), user.getName(), bannerName); // Unban all bans done before
                db.ban(user.getName(), reason, bannerName, Util.getUniqueId(user.getName(), db), type);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        setBanner(user, banner);
        setReason(user, reason);
        setTimeOut(user, timeOut);
    }

    @Override
    public void unban(IngameUser user) {
        unban(user, null);
    }

    @Override
    public void unban(IngameUser user, SinkUser unbanner) {
        try {
            if(getTimeOut(user) != null) {
                db.unbanTempBan(Util.getUniqueId(user.getName(), db), user.getName(), System.currentTimeMillis());
            } else {
                db.unban(Util.getUniqueId(user.getName(), db), user.getName(), unbanner.getDisplayName()); // Unban all bans done before
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setUnbanner(user, unbanner);
    }

    @Override
    public boolean isBanned(IngameUser user) {
        try {
            List<BanData> datas = db.getBanData(Util.getUniqueId(user.getName(), db).toString(), false);
            if(datas == null) return false;
            for (BanData data : datas) {
                if(data.isBanned()) {
                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUnbanTime(IngameUser user, @Nullable Long unbanTimestamp) {
        if(unbanTimestamp == null) {
            unbanTimestamp = -1L;
        }
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return;
        try {
            db.execute("UPDATE " + MySQLDatabase.PLAYER_TABLE + " SET `unbantimestamp` = ? WHERE `id` = ?", unbanTimestamp, lastBan.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private BanData getLastBan(IngameUser user){
        try {
            List<BanData> banData = db.getBanData(user.getUniqueId().toString(), false);
            BanData lastBan = null;
            for(BanData data : banData) {
                if((lastBan == null || data.getId() > lastBan.getId()) && data.isBanned()) {
                    lastBan = data;
                }
            }

            return lastBan;
        } catch (SQLException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void setReason(IngameUser user, String reason) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return;
        try {
            db.execute("UPDATE " + MySQLDatabase.PLAYER_TABLE + " SET `reason` = ? WHERE `id` = ?", reason, lastBan.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public String getReason(IngameUser user) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.getReason();
    }

    @Nullable
    @Override
    public Long getBanTime(IngameUser user) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.getBanTimeStamp();
    }

    @Nullable
    @Override
    public Long getUnbanTime(IngameUser user) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.getUnbanTimeStamp();
    }

    @Override
    public Long getTimeOut(IngameUser user) {
        return getUnbanTime(user);
    }

    @Override
    public void setTimeOut(IngameUser user, @Nullable Long timeOut) {
        setUnbanTime(user, timeOut);
    }

    @Nullable
    @Override
    public String getBannerDisplayName(IngameUser user) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.getBanner();
    }

    @Nullable
    @Override
    public UUID getBannerUniqueId(IngameUser user) {
        return null; // not implemented
    }

    @Nullable
    @Override
    public String getUnbannerDisplayName(IngameUser user) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.getUnbanner();
    }

    @Nullable
    @Override
    public UUID getUnbannerUniqueId(IngameUser user) {
        return null; // not implemented
    }

    @Override
    public void setBanner(IngameUser user, SinkUser banner) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return;
        try {
            db.execute("UPDATE " + MySQLDatabase.PLAYER_TABLE + " SET `bannedby` = ? WHERE `id` = ?", banner == null ? null : banner.getName(), lastBan.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUnbanner(IngameUser user, SinkUser unbanner) {
        BanData lastBan = getLastBan(user);
        if(lastBan == null) return;
        try {
            db.execute("UPDATE " + MySQLDatabase.PLAYER_TABLE + " SET `unbannedby` = ? WHERE `id` = ?", unbanner.getDisplayName(), lastBan.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
