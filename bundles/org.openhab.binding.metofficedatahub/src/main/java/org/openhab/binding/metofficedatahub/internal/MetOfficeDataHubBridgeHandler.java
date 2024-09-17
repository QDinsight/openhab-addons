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

import static org.openhab.binding.metofficedatahub.internal.MetOfficeDataHubBindingConstants.BRIDGE_PROP_FORECAST_REQUEST_COUNT;
import static org.openhab.binding.metofficedatahub.internal.MetOfficeDataHubBindingConstants.DAY_IN_MILLIS;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.org.openhab.binding.metofficedatahub.internal.RequestLimiter;

/**
 * The {@link MetOfficeDataHubBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public class MetOfficeDataHubBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(MetOfficeDataHubBridgeHandler.class);

    protected final RequestLimiter forecastDataLimiter = new RequestLimiter();

    private volatile MetOfficeDataHubBridgeConfiguration config = getConfigAs(
            MetOfficeDataHubBridgeConfiguration.class);

    private @Nullable ScheduledFuture<?> timerResetScheduler = null;
    private final Object timerResetSchedulerLock = new Object();

    public MetOfficeDataHubBridgeHandler(final Bridge bridge) {
        super(bridge);
    }

    public void updateLimiterStats() {
        final Map<String, String> newProps = new HashMap<>();
        newProps.put(BRIDGE_PROP_FORECAST_REQUEST_COUNT, String.valueOf(forecastDataLimiter.getCurrentRequestCount()));
        this.updateProperties(newProps);
    }

    protected String getApiKey() {
        return config.siteSpecificApiKey;
    }

    private static long getMillisUntilMidnight() {
        return Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).toMillis();
    }

    protected static long getMillisSinceDayStart() {
        return Duration.between(LocalDate.now().atStartOfDay(), LocalDateTime.now()).toMillis();
    }

    @Override
    public void initialize() {
        updateLimiterStats();
        config = getConfigAs(MetOfficeDataHubBridgeConfiguration.class);
        forecastDataLimiter.updateLimit(config.siteSpecificRateDailyLimit);

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> updateStatus(ThingStatus.ONLINE));

        scheduleResetDailyLimiters();
    }

    @Override
    public void dispose() {
        cancelResetDailyLimiters();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    /**
     * Forecast Data Limiter Reset Scheduling
     */

    private void scheduleResetDailyLimiters() {
        logger.trace("Scheduling reset of forecast data limiter");
        cancelResetDailyLimiters();
        long delayUntilResetCounters = getMillisUntilMidnight();
        synchronized (timerResetSchedulerLock) {
            timerResetScheduler = scheduler.scheduleWithFixedDelay(() -> {
                logger.trace("Resetting forecast request data limiter");
                forecastDataLimiter.resetLimiter();
            }, delayUntilResetCounters, DAY_IN_MILLIS, TimeUnit.MILLISECONDS);
        }
        logger.trace("Scheduled reset of forecast data limiter complete");
    }

    private void cancelResetDailyLimiters() {
        synchronized (timerResetSchedulerLock) {
            ScheduledFuture<?> job = timerResetScheduler;
            if (job != null) {
                job.cancel(true);
                timerResetScheduler = null;
                logger.trace("Cancelled scheduled reset of forecast data limiter");
            }
        }
    }
}
