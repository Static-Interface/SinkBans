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

package de.static_interface.sinkbans.model;

import de.static_interface.sinklibrary.user.IngameUser;

public class BanRequestData implements Comparable<BanRequestData> {
    public int id;
    public String creatorName;
    public IngameUser creator;
    public IngameUser target;
    public String closerName;
    public IngameUser closer;
    public String reason;
    public int state;
    public long timeCreated;
    public long timeClosed;
    public long unbantime;

    @Override
    public int compareTo(BanRequestData o) {
        if(o == null) return 1;
        return Integer.valueOf(id).compareTo(o.id);
    }
}
