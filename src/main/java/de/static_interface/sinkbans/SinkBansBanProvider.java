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
import static de.static_interface.sinklibrary.database.query.Query.eq;
import static de.static_interface.sinklibrary.database.query.Query.from;

import de.static_interface.sinkbans.database.BanRow;
import de.static_interface.sinkbans.database.DatabaseBanManager;
import de.static_interface.sinklibrary.api.provider.BanProvider;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.Debug;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class SinkBansBanProvider implements BanProvider {
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
        Debug.logMethodCall(user, banner == null ? null : banner.getName(), reason, timeOut);
        String bannerName = banner == null ? null : banner.getName();

        if(timeOut != null) {
            DatabaseBanManager.unbanTempBan(user.getUniqueId(), bannerName); // Unban all bans done before
            DatabaseBanManager.tempBan(user, timeOut, bannerName);
        }
         else {
            DatabaseBanManager.unban(user.getUniqueId(), bannerName); // Unban all bans done before
            DatabaseBanManager.unbanTempBan(user.getUniqueId(), bannerName);
            DatabaseBanManager.ban(user, reason, bannerName);
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
        Debug.logMethodCall(user == null ? null : user.getName(), unbanner == null ? null : unbanner.getName());
        String unbannerName = unbanner == null ? null : unbanner.getName();
        if(getTimeOut(user) != null) {
            DatabaseBanManager.unbanTempBan(user.getUniqueId(), unbannerName);
        } else {
            DatabaseBanManager.unban(user.getUniqueId(), unbannerName);
        }
        setUnbanner(user, unbanner);
    }

    @Override
    public boolean isBanned(IngameUser user) {
        List<BanRow> datas = DatabaseBanManager.getUserBans(user);
        if(datas == null) return false;
        for (BanRow data : datas) {
            if(data.isbanned) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setUnbanTime(IngameUser user, @Nullable Long unbanTimestamp) {
        if(unbanTimestamp == null) {
            unbanTimestamp = -1L;
        }
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return;
        from(SinkBans.getInstance().getBanTable())
                .update()
                .set("unbantimestamp", "?")
                .where("id", eq("?"))
                .execute(unbanTimestamp, lastBan.id);
    }

    @Nullable
    private BanRow getLastBan(IngameUser user){
        List<BanRow> banData = DatabaseBanManager.getUserBans(user);
        BanRow lastBan = null;
        for(BanRow data : banData) {
            if((lastBan == null || data.id  > lastBan.id) && data.isbanned) {
                lastBan = data;
            }
        }

        return lastBan;
    }

    @Override
    public void setReason(IngameUser user, String reason) {
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return;
        from(SinkBans.getInstance().getBanTable())
                .update()
                .set("reason", "?")
                .where("id", eq("?"))
                .execute(reason, lastBan.id);
    }

    @Nullable
    @Override
    public String getReason(IngameUser user) {
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.reason;
    }

    @Nullable
    @Override
    public Long getBanTime(IngameUser user) {
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.bantimestamp;
    }

    @Nullable
    @Override
    public Long getUnbanTime(IngameUser user) {
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.unbantimestamp <= 0 ? null : lastBan.unbantimestamp;
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
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.bannedby;
    }

    @Nullable
    @Override
    public UUID getBannerUniqueId(IngameUser user) {
        return null; // not implemented
    }

    @Nullable
    @Override
    public String getUnbannerDisplayName(IngameUser user) {
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return null;
        return lastBan.unbannedby;
    }

    @Nullable
    @Override
    public UUID getUnbannerUniqueId(IngameUser user) {
        return null; // not implemented
    }

    @Override
    public void setBanner(IngameUser user, SinkUser banner) {
        Debug.logMethodCall(user.getName(), banner == null ? null : banner.getName());
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return;
        from(SinkBans.getInstance().getBanTable())
                .update()
                .set("bannedby", "?")
                .where("id", eq("?"))
                .execute(banner.getName(), lastBan.id);
    }

    @Override
    public void setUnbanner(IngameUser user, SinkUser unbanner) {
        Debug.logMethodCall(user.getName(), unbanner == null ? null : unbanner.getName());
        BanRow lastBan = getLastBan(user);
        if(lastBan == null) return;
        from(SinkBans.getInstance().getBanTable())
                .update()
                .set("unbannedby", "?")
                .where("id", eq("?"))
                .execute(unbanner.getName(), lastBan.id);
    }
}
