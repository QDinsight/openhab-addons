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
package org.openhab.binding.solarforecast.internal.solcast.handler;

import static org.openhab.binding.solarforecast.internal.SolarForecastBindingConstants.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.solarforecast.internal.actions.SolarForecast;
import org.openhab.binding.solarforecast.internal.actions.SolarForecastActions;
import org.openhab.binding.solarforecast.internal.actions.SolarForecastProvider;
import org.openhab.binding.solarforecast.internal.solcast.SolcastObject;
import org.openhab.binding.solarforecast.internal.solcast.SolcastObject.QueryMode;
import org.openhab.binding.solarforecast.internal.solcast.config.SolcastBridgeConfiguration;
import org.openhab.binding.solarforecast.internal.utils.Utils;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TimeSeries.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SolcastBridgeHandler} is a non active handler instance. It will be triggered by the bridge.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class SolcastBridgeHandler extends BaseBridgeHandler implements SolarForecastProvider, TimeZoneProvider {
    private final Logger logger = LoggerFactory.getLogger(SolcastBridgeHandler.class);

    private List<SolcastPlaneHandler> planes = new ArrayList<SolcastPlaneHandler>();
    private Optional<SolcastBridgeConfiguration> configuration = Optional.empty();
    private Optional<ScheduledFuture<?>> refreshJob = Optional.empty();
    private ZoneId timeZone;

    public SolcastBridgeHandler(Bridge bridge, TimeZoneProvider tzp) {
        super(bridge);
        timeZone = tzp.getTimeZone();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return List.of(SolarForecastActions.class);
    }

    @Override
    public void initialize() {
        SolcastBridgeConfiguration config = getConfigAs(SolcastBridgeConfiguration.class);
        configuration = Optional.of(config);
        if (!EMPTY.equals(config.apiKey)) {
            if (!configuration.get().timeZone.isEmpty()) {
                try {
                    timeZone = ZoneId.of(configuration.get().timeZone);
                } catch (DateTimeException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "@text/solarforecast.site.status.timezone" + " [\"" + configuration.get().timeZone + "\"]");
                    return;
                }
            }
            updateStatus(ThingStatus.ONLINE);
            refreshJob = Optional
                    .of(scheduler.scheduleWithFixedDelay(this::getData, 0, REFRESH_ACTUAL_INTERVAL, TimeUnit.MINUTES));
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/solarforecast.site.status.api-key-missing");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            String channel = channelUID.getIdWithoutGroup();
            switch (channel) {
                case CHANNEL_ENERGY_ACTUAL:
                case CHANNEL_ENERGY_REMAIN:
                case CHANNEL_ENERGY_TODAY:
                case CHANNEL_POWER_ACTUAL:
                    getData();
                    break;
                case CHANNEL_POWER_ESTIMATE:
                case CHANNEL_POWER_ESTIMATE10:
                case CHANNEL_POWER_ESTIMATE90:
                case CHANNEL_ENERGY_ESTIMATE:
                case CHANNEL_ENERGY_ESTIMATE10:
                case CHANNEL_ENERGY_ESTIMATE90:
                    forecastUpdate();
                    break;
            }
        }
    }

    @Override
    public void dispose() {
        refreshJob.ifPresent(job -> job.cancel(true));
    }

    /**
     * Get data for all planes. Protect parts map from being modified during update
     */
    public synchronized void getData() {
        if (planes.isEmpty()) {
            logger.debug("No PV plane defined yet");
            return;
        }
        ZonedDateTime now = ZonedDateTime.now(getTimeZone());
        double energySum = 0;
        double powerSum = 0;
        double daySum = 0;
        for (Iterator<SolcastPlaneHandler> iterator = planes.iterator(); iterator.hasNext();) {
            SolcastPlaneHandler sfph = iterator.next();
            SolcastObject fo = sfph.fetchData();
            energySum += fo.getActualEnergyValue(now, QueryMode.Estimation);
            powerSum += fo.getActualPowerValue(now, QueryMode.Estimation);
            daySum += fo.getDayTotal(now.toLocalDate(), QueryMode.Estimation);
        }
        updateState(CHANNEL_ENERGY_ACTUAL, Utils.getEnergyState(energySum));
        updateState(CHANNEL_ENERGY_REMAIN, Utils.getEnergyState(daySum - energySum));
        updateState(CHANNEL_ENERGY_TODAY, Utils.getEnergyState(daySum));
        updateState(CHANNEL_POWER_ACTUAL, Utils.getPowerState(powerSum));
    }

    public void forecastUpdate() {
        if (planes.isEmpty()) {
            return;
        }
        // get all available forecasts
        List<SolarForecast> forecastObjects = new ArrayList<SolarForecast>();
        for (Iterator<SolcastPlaneHandler> iterator = planes.iterator(); iterator.hasNext();) {
            SolcastPlaneHandler sfph = iterator.next();
            forecastObjects.addAll(sfph.getSolarForecasts());
        }
        // sort in Tree according to times for each scenario
        List<QueryMode> modes = List.of(QueryMode.Estimation, QueryMode.Pessimistic, QueryMode.Optimistic);
        modes.forEach(mode -> {
            TreeMap<Instant, QuantityType<?>> combinedPowerForecast = new TreeMap<Instant, QuantityType<?>>();
            TreeMap<Instant, QuantityType<?>> combinedEnergyForecast = new TreeMap<Instant, QuantityType<?>>();
            forecastObjects.forEach(fc -> {
                TimeSeries powerTS = fc.getPowerTimeSeries(mode);
                powerTS.getStates().forEach(entry -> {
                    Utils.addState(combinedPowerForecast, entry);
                });
                TimeSeries energyTS = fc.getEnergyTimeSeries(mode);
                energyTS.getStates().forEach(entry -> {
                    Utils.addState(combinedEnergyForecast, entry);
                });
            });
            // create TimeSeries and distribute
            TimeSeries powerSeries = new TimeSeries(Policy.REPLACE);
            combinedPowerForecast.forEach((timestamp, state) -> {
                powerSeries.add(timestamp, state);
            });

            TimeSeries energySeries = new TimeSeries(Policy.REPLACE);
            combinedEnergyForecast.forEach((timestamp, state) -> {
                energySeries.add(timestamp, state);
            });
            switch (mode) {
                case Estimation:
                    sendTimeSeries(CHANNEL_ENERGY_ESTIMATE, energySeries);
                    sendTimeSeries(CHANNEL_POWER_ESTIMATE, powerSeries);
                    break;
                case Optimistic:
                    sendTimeSeries(CHANNEL_ENERGY_ESTIMATE90, energySeries);
                    sendTimeSeries(CHANNEL_POWER_ESTIMATE90, powerSeries);
                    break;
                case Pessimistic:
                    sendTimeSeries(CHANNEL_ENERGY_ESTIMATE10, energySeries);
                    sendTimeSeries(CHANNEL_POWER_ESTIMATE10, powerSeries);
                    break;
                default:
                    break;
            }
        });
    }

    public synchronized void addPlane(SolcastPlaneHandler sph) {
        planes.add(sph);
    }

    public synchronized void removePlane(SolcastPlaneHandler sph) {
        planes.remove(sph);
    }

    String getApiKey() {
        if (configuration.isPresent()) {
            return configuration.get().apiKey;
        }
        return EMPTY;
    }

    @Override
    public synchronized List<SolarForecast> getSolarForecasts() {
        List<SolarForecast> l = new ArrayList<SolarForecast>();
        planes.forEach(entry -> {
            l.addAll(entry.getSolarForecasts());
        });
        return l;
    }

    @Override
    public ZoneId getTimeZone() {
        return timeZone;
    }
}
