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
package org.openhab.binding.solarforecast.internal.actions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.measure.MetricPrefix;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Power;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.solarforecast.internal.utils.Utils;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actions to query forecast objects
 *
 * @author Bernd Weymann - Initial contribution
 */
@ThingActionsScope(name = "solarforecast")
@NonNullByDefault
public class SolarForecastActions implements ThingActions {
    private final Logger logger = LoggerFactory.getLogger(SolarForecastActions.class);
    private Optional<ThingHandler> thingHandler = Optional.empty();

    @RuleAction(label = "@text/actionDayLabel", description = "@text/actionDayDesc")
    public QuantityType<Energy> getDay(
            @ActionInput(name = "localDate", label = "@text/actionInputDayLabel", description = "@text/actionInputDayDesc") LocalDate localDate,
            String... args) {
        if (thingHandler.isPresent()) {
            List<SolarForecast> l = ((SolarForecastProvider) thingHandler.get()).getSolarForecasts();
            if (!l.isEmpty()) {
                QuantityType<Energy> measure = QuantityType.valueOf(0, Units.KILOWATT_HOUR);
                for (Iterator<SolarForecast> iterator = l.iterator(); iterator.hasNext();) {
                    SolarForecast solarForecast = iterator.next();
                    State s = solarForecast.getDay(localDate, args);
                    if (s instanceof QuantityType<?> quantityState) {
                        measure = measure.add((QuantityType<Energy>) quantityState);
                    } else {
                        // break in case of failure getting values to avoid ambiguous values
                        logger.debug("Ambiguous measure {} found for {}", s, localDate);
                        return Utils.getEnergyState(-1);
                    }
                }
                return measure;
            } else {
                logger.debug("No forecasts found for {}", localDate);
                return Utils.getEnergyState(-1);
            }
        } else {
            logger.trace("Handler missing");
            return Utils.getEnergyState(-1);
        }
    }

    @RuleAction(label = "@text/actionPowerLabel", description = "@text/actionPowerDesc")
    public QuantityType<Power> getPower(
            @ActionInput(name = "timestamp", label = "@text/actionInputDateTimeLabel", description = "@text/actionInputDateTimeDesc") Instant timestamp,
            String... args) {
        if (thingHandler.isPresent()) {
            List<SolarForecast> l = ((SolarForecastProvider) thingHandler.get()).getSolarForecasts();
            if (!l.isEmpty()) {
                QuantityType<Power> measure = QuantityType.valueOf(0, MetricPrefix.KILO(Units.WATT));
                for (Iterator<SolarForecast> iterator = l.iterator(); iterator.hasNext();) {
                    SolarForecast solarForecast = iterator.next();
                    State s = solarForecast.getPower(timestamp, args);
                    if (s instanceof QuantityType<?> quantityState) {
                        measure = measure.add((QuantityType<Power>) quantityState);
                    } else {
                        // break in case of failure getting values to avoid ambiguous values
                        logger.debug("Ambiguous measure {} found for {}", s, timestamp);
                        return Utils.getPowerState(-1);
                    }
                }
                return measure;
            } else {
                logger.debug("No forecasts found for {}", timestamp);
                return Utils.getPowerState(-1);
            }
        } else {
            logger.trace("Handler missing");
            return Utils.getPowerState(-1);
        }
    }

    @RuleAction(label = "@text/actionEnergyLabel", description = "@text/actionEnergyDesc")
    public QuantityType<Energy> getEnergy(
            @ActionInput(name = "start", label = "@text/actionInputDateTimeBeginLabel", description = "@text/actionInputDateTimeBeginDesc") Instant start,
            @ActionInput(name = "end", label = "@text/actionInputDateTimeEndLabel", description = "@text/actionInputDateTimeEndDesc") Instant end,
            String... args) {
        if (thingHandler.isPresent()) {
            List<SolarForecast> l = ((SolarForecastProvider) thingHandler.get()).getSolarForecasts();
            if (!l.isEmpty()) {
                QuantityType<Energy> measure = QuantityType.valueOf(0, Units.KILOWATT_HOUR);
                for (Iterator<SolarForecast> iterator = l.iterator(); iterator.hasNext();) {
                    SolarForecast solarForecast = iterator.next();
                    State s = solarForecast.getEnergy(start, end, args);
                    if (s instanceof QuantityType<?> quantityState) {
                        measure = measure.add((QuantityType<Energy>) quantityState);
                    } else {
                        // break in case of failure getting values to avoid ambiguous values
                        logger.debug("Ambiguous measure {} found between {} and {}", s, start, end);
                        return Utils.getEnergyState(-1);
                    }
                }
                return measure;
            } else {
                logger.debug("No forecasts found for between {} and {}", start, end);
                return Utils.getEnergyState(-1);
            }
        } else {
            logger.trace("Handler missing");
            return Utils.getEnergyState(-1);
        }
    }

    @RuleAction(label = "@text/actionForecastBeginLabel", description = "@text/actionForecastBeginDesc")
    public Instant getForecastBegin() {
        Instant returnBeginTime = Instant.MAX;
        if (thingHandler.isPresent()) {
            List<SolarForecast> l = ((SolarForecastProvider) thingHandler.get()).getSolarForecasts();
            if (!l.isEmpty()) {
                for (Iterator<SolarForecast> iterator = l.iterator(); iterator.hasNext();) {
                    SolarForecast solarForecast = iterator.next();
                    Instant begin = solarForecast.getForecastBegin();
                    // break in case of failure getting values to avoid ambiguous values
                    if (begin.equals(Instant.MAX)) {
                        return Instant.MAX;
                    }
                    // take latest possible timestamp to avoid ambiguous values
                    if (begin.isBefore(returnBeginTime)) {
                        returnBeginTime = begin;
                    }
                }
                return returnBeginTime;
            } else {
                logger.trace("No forecasts found - return invalid date MAX");
                return returnBeginTime;
            }
        } else {
            logger.trace("Handler missing - return invalid date MAX");
            return returnBeginTime;
        }
    }

    @RuleAction(label = "@text/actionForecastEndLabel", description = "@text/actionForecastEndDesc")
    public Instant getForecastEnd() {
        Instant returnEndTime = Instant.MIN;
        if (thingHandler.isPresent()) {
            List<SolarForecast> l = ((SolarForecastProvider) thingHandler.get()).getSolarForecasts();
            if (!l.isEmpty()) {
                for (Iterator<SolarForecast> iterator = l.iterator(); iterator.hasNext();) {
                    SolarForecast solarForecast = iterator.next();
                    Instant forecastEndTime = solarForecast.getForecastEnd();
                    // break in case of failure getting values to avoid ambiguous values
                    if (forecastEndTime.equals(Instant.MIN)) {
                        return Instant.MIN;
                    }
                    // take earliest possible timestamp to avoid ambiguous values
                    if (forecastEndTime.isAfter(returnEndTime)) {
                        returnEndTime = forecastEndTime;
                    }
                }
                return returnEndTime;
            } else {
                logger.trace("No forecasts found - return invalid date MIN");
                return returnEndTime;
            }
        } else {
            logger.trace("Handler missing - return invalid date MIN");
            return returnEndTime;
        }
    }

    public static State getDay(ThingActions actions, LocalDate ld, String... args) {
        return ((SolarForecastActions) actions).getDay(ld, args);
    }

    public static State getPower(ThingActions actions, Instant dateTime, String... args) {
        return ((SolarForecastActions) actions).getPower(dateTime, args);
    }

    public static State getEnergy(ThingActions actions, Instant begin, Instant end, String... args) {
        return ((SolarForecastActions) actions).getEnergy(begin, end, args);
    }

    public static Instant getForecastBegin(ThingActions actions) {
        return ((SolarForecastActions) actions).getForecastBegin();
    }

    public static Instant getForecastEnd(ThingActions actions) {
        return ((SolarForecastActions) actions).getForecastEnd();
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        thingHandler = Optional.of(handler);
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        if (thingHandler.isPresent()) {
            return thingHandler.get();
        }
        return null;
    }
}
