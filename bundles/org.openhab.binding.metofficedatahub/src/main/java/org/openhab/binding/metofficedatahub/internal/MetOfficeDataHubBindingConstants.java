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
package org.openhab.binding.metofficedatahub.internal;

import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link MetOfficeDataHubBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class MetOfficeDataHubBindingConstants {

    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting()
            .disableHtmlEscaping().serializeNulls().create();

    private static final String BINDING_ID = "metofficedatahub";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_SITE_SPEC_API = new ThingTypeUID(BINDING_ID, "siteSpecificApi");

    /**
     * Site Specific API - Shared
     */

    public static final String SITE_TIMESTAMP = "forecast-ts";

    /**
     * Site Specific API - Hourly Forecast Channel Names
     */
    public static final String SITE_HOURLY_FORECAST_SCREEN_TEMPERATURE = "site-scn-temp";

    public static final String SITE_HOURLY_FORECAST_MIN_SCREEN_TEMPERATURE = "site-min-scn-temp";
    public static final String SITE_HOURLY_FORECAST_MAX_SCREEN_TEMPERATURE = "site-max-scn-temp";

    public static final String SITE_HOURLY_FEELS_LIKE_TEMPERATURE = "feels-like-temp";

    public static final String SITE_HOURLY_SCREEN_RELATIVE_HUMIDITY = "scn-rel-humidity";

    public static final String SITE_HOURLY_VISIBILITY = "visibility";

    public static final String SITE_HOURLY_PROBABILITY_OF_PRECIPITATION = "prob-precip";

    public static final String SITE_HOURLY_PRECIPITATION_RATE = "precip-rate";

    public static final String SITE_HOURLY_TOTAL_PRECIPITATION_AMOUNT = "total-precip";

    public static final String SITE_HOURLY_TOTAL_SNOW_AMOUNT = "total-snow";

    public static final String SITE_HOURLY_UV_INDEX = "uv-index";

    public static final String SITE_HOURLY_MSLP = "mslp";

    public static final String SITE_HOURLY_WIND_SPEED_10M = "wind-sp-10m";

    public static final String SITE_HOURLY_WIND_GUST_SPEED_10M = "wind-gst-sp-10m";

    public static final String SITE_HOURLY_MAX_10M_WIND_GUST = "max-10m-wind-gst";

    public static final String SITE_HOURLY_WIND_DIRECTION_FROM_10M = "wind-dir-10m";

    public static final String SITE_HOURLY_SCREEN_DEW_POINT_TEMPERATURE = "scn-dew-temp";

    public static final String SITE_HOURLY_LOCATION_NAME = "loc-name";

    public static final String SITE_DAILY_MIDDAY_WIND_SPEED_10M = "midday-wind-sp-10m";

    public static final String SITE_DAILY_MIDNIGHT_WIND_SPEED_10M = "midnight-wind-sp-10m";

    public static final String SITE_DAILY_MIDDAY_WIND_DIRECTION_10M = "midday-10m-wind-dir";
    public static final String SITE_DAILY_MIDNIGHT_WIND_DIRECTION_10M = "midnight-10m-wind-dir";

    public static final String SITE_DAILY_MIDDAY_WIND_GUST_10M = "midday-10m-wind-gst";

    public static final String SITE_DAILY_MIDNIGHT_WIND_GUST_10M = "midnight-10m-wind-gst";

    public static final String SITE_DAILY_MIDDAY_VISIBILITY = "midday-vis";

    public static final String SITE_DAILY_MIDNIGHT_VISIBILITY = "midnight-vis";

    public static final String SITE_DAILY_MIDDAY_REL_HUMIDITY = "midday-rel-hum";

    public static final String SITE_DAILY_MIDNIGHT_REL_HUMIDITY = "midnight-rel-hum";

    public static final String SITE_DAILY_MIDDAY_MSLP = "midday-mslp";
    public static final String SITE_DAILY_MIDNIGHT_MSLP = "midnight-mslp";

    public static final String SITE_DAILY_DAY_MAX_UV_INDEX = "max-uv-idx";

    public static final String SITE_DAILY_DAY_UPPER_BOUND_MAX_TEMP = "day-ub-max-temp";
    public static final String SITE_DAILY_DAY_LOWER_BOUND_MAX_TEMP = "day-lb-max-temp";

    public static final String SITE_DAILY_NIGHT_UPPER_BOUND_MAX_TEMP = "night-ub-min-temp";
    public static final String SITE_DAILY_NIGHT_LOWER_BOUND_MAX_TEMP = "night-lb-min-temp";

    public static final String SITE_DAILY_NIGHT_FEELS_LIKE_MIN_TEMP = "night-min-feels-temp";

    public static final String SITE_DAILY_DAY_FEELS_LIKE_MAX_TEMP = "day-max-feels-temp";

    public static final String SITE_DAILY_NIGHT_LOWER_BOUND_MIN_TEMP = "night-lb-min-temp";

    public static final String SITE_DAILY_DAY_MAX_FEELS_LIKE_TEMP = "day-max-feels-temp";

    public static final String SITE_DAILY_NIGHT_LOWER_BOUND_MIN_FEELS_LIKE_TEMP = "night-lb-min-feels-temp";

    public static final String SITE_DAILY_DAY_LOWER_BOUND_MAX_FEELS_LIKE_TEMP = "day-lb-max-feels-temp";

    public static final String SITE_DAILY_DAY_UPPER_BOUND_MAX_FEELS_LIKE_TEMP = "day-ub-max-feels-temp";

    public static final String SITE_DAILY_UPPER_BOUND_MIN_FEELS_LIKE_TEMP = "night-ub-min-feels-temp";

    public static final String SITE_DAILY_DAY_PROBABILITY_OF_PRECIPITATION = "day-prob-precip";

    public static final String SITE_DAILY_NIGHT_PROBABILITY_OF_PRECIPITATION = "night-prob-precip";

    public static final String SITE_DAILY_DAY_PROBABILITY_OF_SNOW = "day-prob-snow";

    public static final String SITE_DAILY_NIGHT_PROBABILITY_OF_SNOW = "night-prob-snow";

    public static final String SITE_DAILY_DAY_PROBABILITY_OF_HEAVY_SNOW = "day-prob-heavy-snow";

    public static final String SITE_DAILY_NIGHT_PROBABILITY_OF_HEAVY_SNOW = "night-prob-heavy-snow";

    public static final String SITE_DAILY_DAY_PROBABILITY_OF_RAIN = "day-prob-rain";

    public static final String SITE_DAILY_NIGHT_PROBABILITY_OF_RAIN = "night-prob-rain";

    public static final String SITE_DAILY_DAY_PROBABILITY_OF_HEAVY_RAIN = "day-prob-heavy-rain";

    public static final String SITE_DAILY_NIGHT_PROBABILITY_OF_HEAVY_RAIN = "night-prob-heavy-rain";

    public static final String SITE_DAILY_DAY_PROBABILITY_OF_HAIL = "day-prob-hail";

    public static final String SITE_DAILY_NIGHT_PROBABILITY_OF_HAIL = "night-prob-hail";

    public static final String SITE_DAILY_DAY_PROBABILITY_OF_SFERICS = "day-prob-sferics";

    public static final String SITE_DAILY_NIGHT_PROBABILITY_OF_SFERICS = "night-prob-sferics";

    public static final String SITE_DAILY_DAY_MAX_SCREEN_TEMPERATURE = "day-max-scn-temp";
    public static final String SITE_DAILY_NIGHT_MIN_SCREEN_TEMPERATURE = "night-min-scn-temp";

    public static final String GROUP_PREFIX_HOURS_FORECAST = "current-forecast";
    public static final String GROUP_PREFIX_DAILY_FORECAST = "daily-forecast";
    public static final String GROUP_POSTFIX_BOTH_FORECASTS = "-plus";
    public static final char GROUP_PREFIX_TO_ITEM = '#';

    public static final String GET_FORECAST_URL_DAILY = "https://data.hub.api.metoffice.gov.uk/sitespecific/v0/point/daily?includeLocationName=true&latitude=<LATITUDE>&longitude=<LONGITUDE>";
    public static final String GET_FORECAST_URL_HOURLY = "https://data.hub.api.metoffice.gov.uk/sitespecific/v0/point/hourly?includeLocationName=true&latitude=<LATITUDE>&longitude=<LONGITUDE>";

    public static final long DAY_IN_MILLIS = 86400000;

    public static final Random RANDOM_GENERATOR = new Random();

    public static final String BRIDGE_PROP_FORECAST_REQUEST_COUNT = "Site Specific API Call Count";
}
