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

package de.static_interface.sinkbans.database;

import de.static_interface.sinklibrary.database.Row;
import de.static_interface.sinklibrary.database.annotation.Column;

import javax.annotation.Nullable;

public class BanRequest implements Row {
    @Column(primaryKey = true, autoIncrement = true)
    public Integer id;

    @Column
    public String creator;

    @Column
    @Nullable
    public String creator_uuid;

    @Column
    public String reason;

    @Column
    public String target_uuid;

    @Column
    public String closer;

    @Column
    @Nullable
    public String closer_uuid;

    @Column
    public int state;

    @Column
    public long time_created;

    @Column
    public long time_closed;

    @Column
    @Nullable
    public Long unbantime; //Todo implement this
}
