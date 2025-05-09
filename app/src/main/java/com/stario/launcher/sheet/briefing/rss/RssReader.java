/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.sheet.briefing.rss;

import com.apptasticsoftware.rssreader.Channel;
import com.apptasticsoftware.rssreader.DateTimeParser;
import com.apptasticsoftware.rssreader.Item;

class RssReader extends WoodstoxAbstractRssReader<Channel, Item> {
    public RssReader() {
        super();
    }

    @Override
    protected Channel createChannel(DateTimeParser dateTimeParser) {
        return new Channel(dateTimeParser);
    }

    @Override
    protected Item createItem(DateTimeParser dateTimeParser) {
        return new Item(dateTimeParser);
    }

}