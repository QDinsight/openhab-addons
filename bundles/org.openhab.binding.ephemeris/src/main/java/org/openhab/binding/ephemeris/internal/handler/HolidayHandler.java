/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.ephemeris.internal.handler;

import static org.openhab.binding.ephemeris.internal.EphemerisBindingConstants.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ephemeris.internal.providers.EphemerisDescriptionProvider;
import org.openhab.core.ephemeris.EphemerisManager;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.State;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;

/**
 * The {@link HolidayHandler} delivers system default Holidays data.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class HolidayHandler extends JollydayHandler {
    private final List<StateOption> events = new ArrayList<>();

    public HolidayHandler(Thing thing, EphemerisManager ephemerisManager, ZoneId zoneId,
            EphemerisDescriptionProvider descriptionProvider) {
        super(thing, ephemerisManager, zoneId);

        // Search all holidays in the coming year
        ZonedDateTime now = ZonedDateTime.now();
        for (int offset = 0; offset < 366; offset++) {
            String nextEvent = getEvent(now.plusDays(offset));
            if (nextEvent != null) {
                String description = ephemeris.getHolidayDescription(nextEvent);
                events.add(new StateOption(nextEvent, description == null ? nextEvent : description));
            }
        }

        // Set descriptions for these events
        descriptionProvider.setStateOptions(new ChannelUID(thing.getUID(), CHANNEL_CURRENT_EVENT), events);
        descriptionProvider.setStateOptions(new ChannelUID(thing.getUID(), CHANNEL_NEXT_EVENT), events);
    }

    @Override
    protected @Nullable String internalUpdate(ZonedDateTime today) {
        String event = getEvent(today);
        updateState(CHANNEL_HOLIDAY_TODAY, OnOffType.from(event != null));

        event = getEvent(today.plusDays(1));
        updateState(CHANNEL_HOLIDAY_TOMORROW, OnOffType.from(event != null));
        return super.internalUpdate(today);
    }

    @Override
    protected @Nullable String getEvent(ZonedDateTime day) {
        return ephemeris.getBankHolidayName(day);
    }

    @Override
    protected State toStringType(@Nullable String event) {
        return event == null || event.isEmpty() ? UnDefType.NULL : new StringType(event);
    }
}
