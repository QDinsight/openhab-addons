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
package org.openhab.binding.salus.internal.handler;

import static java.math.RoundingMode.HALF_EVEN;
import static org.openhab.binding.salus.internal.SalusBindingConstants.Channels.It600.EXPECTED_TEMPERATURE;
import static org.openhab.binding.salus.internal.SalusBindingConstants.Channels.It600.TEMPERATURE;
import static org.openhab.binding.salus.internal.SalusBindingConstants.Channels.It600.WORK_TYPE;
import static org.openhab.binding.salus.internal.SalusBindingConstants.It600Device.HoldType.AUTO;
import static org.openhab.binding.salus.internal.SalusBindingConstants.It600Device.HoldType.MANUAL;
import static org.openhab.binding.salus.internal.SalusBindingConstants.It600Device.HoldType.OFF;
import static org.openhab.binding.salus.internal.SalusBindingConstants.It600Device.HoldType.TEMPORARY_MANUAL;
import static org.openhab.binding.salus.internal.SalusBindingConstants.SalusDevice.DSN;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.BRIDGE_UNINITIALIZED;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.salus.internal.rest.DeviceProperty;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Grześlowski - Initial contribution
 */
@NonNullByDefault
public class It600Handler extends BaseThingHandler {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
    private static final Set<String> REQUIRED_CHANNELS = Set.of("ep_9:sIT600TH:LocalTemperature_x100",
            "ep_9:sIT600TH:HeatingSetpoint_x100", "ep_9:sIT600TH:SetHeatingSetpoint_x100", "ep_9:sIT600TH:HoldType",
            "ep_9:sIT600TH:SetHoldType");
    private final Logger logger;
    @NonNullByDefault({})
    private String dsn;
    @NonNullByDefault({})
    private CloudApi cloudApi;

    public It600Handler(Thing thing) {
        super(thing);
        logger = LoggerFactory.getLogger(It600Handler.class.getName() + "[" + thing.getUID().getId() + "]");
    }

    @Override
    public void initialize() {
        {
            var bridge = getBridge();
            if (bridge == null) {
                updateStatus(OFFLINE, BRIDGE_UNINITIALIZED,
                        "There is no bridge for this thing. Remove it and add it again.");
                return;
            }
            if (!(bridge.getHandler() instanceof CloudBridgeHandler cloudHandler)) {
                updateStatus(OFFLINE, BRIDGE_UNINITIALIZED, "There is wrong type of bridge for cloud device!");
                return;
            }
            this.cloudApi = cloudHandler;
        }

        dsn = (String) getConfig().get(DSN);

        if (dsn == null || dsn.length() == 0) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR,
                    "There is no " + DSN + " for this thing. Remove it and add it again.");
            return;
        }

        try {
            var device = this.cloudApi.findDevice(dsn);
            // no device in cloud
            if (device.isEmpty()) {
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Device with DSN " + dsn + " not found!");
                return;
            }
            // device is not connected
            if (!device.get().isConnected()) {
                updateStatus(OFFLINE, COMMUNICATION_ERROR, "Device with DSN " + dsn + " is not connected!");
                return;
            }
            // device is missing properties
            var deviceProperties = findDeviceProperties().stream().map(DeviceProperty::getName).toList();
            var result = new ArrayList<>(REQUIRED_CHANNELS);
            result.removeAll(deviceProperties);
            if (!result.isEmpty()) {
                updateStatus(OFFLINE, CONFIGURATION_ERROR,
                        "Device with DSN " + dsn + " is missing required properties: " + String.join(", ", result));
                return;
            }
        } catch (Exception e) {
            updateStatus(OFFLINE, COMMUNICATION_ERROR, "Error when connecting to Salus Cloud!");
            return;
        }

        // done
        updateStatus(ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        var id = channelUID.getId();
        switch (id) {
            case TEMPERATURE:
                handleCommandForTemperature(channelUID, command);
                break;
            case EXPECTED_TEMPERATURE:
                handleCommandForExpectedTemperature(channelUID, command);
                break;
            case WORK_TYPE:
                handleCommandForWorkType(channelUID, command);
                break;
            default:
                logger.warn("Unknown channel `{}` for command `{}`", id, command);
        }
    }

    private void handleCommandForTemperature(ChannelUID channelUID, Command command) {
        if (!(command instanceof RefreshType)) {
            // only refresh commands are supported for temp channel
            return;
        }

        findLongProperty("ep_9:sIT600TH:LocalTemperature_x100", "LocalTemperature_x100")
                .map(DeviceProperty.LongDeviceProperty::getValue).map(BigDecimal::new)
                .map(value -> value.divide(ONE_HUNDRED, new MathContext(5, HALF_EVEN))).map(DecimalType::new)
                .ifPresent(state -> updateState(channelUID, state));
    }

    private void handleCommandForExpectedTemperature(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            findLongProperty("ep_9:sIT600TH:HeatingSetpoint_x100", "HeatingSetpoint_x100")
                    .map(DeviceProperty.LongDeviceProperty::getValue).map(BigDecimal::new)
                    .map(value -> value.divide(ONE_HUNDRED, new MathContext(5, HALF_EVEN))).map(DecimalType::new)
                    .ifPresent(state -> updateState(channelUID, state));
            return;
        }

        BigDecimal rawValue = null;
        if (command instanceof QuantityType commandAsQuantityType) {
            rawValue = commandAsQuantityType.toBigDecimal();
        } else if (command instanceof DecimalType commandAsDecimalType) {
            rawValue = commandAsDecimalType.toBigDecimal();
        }
        
        if (rawValue != null) {

            var value = rawValue.multiply(ONE_HUNDRED).longValue();
            var property = findLongProperty("ep_9:sIT600TH:SetHeatingSetpoint_x100", "SetHeatingSetpoint_x100");
            if (property.isEmpty()) {
                return;
            }
            var wasSet = cloudApi.setValueForProperty(dsn, property.get().getName(), value);
            if (wasSet) {
                findLongProperty("ep_9:sIT600TH:HeatingSetpoint_x100", "HeatingSetpoint_x100")
                        .ifPresent(prop -> prop.setValue(value));
                findLongProperty("ep_9:sIT600TH:HoldType", "HoldType").ifPresent(prop -> prop.setValue((long) MANUAL));
            }
            return;
        }

        logger.debug("Does not know how to handle command `{}` ({}) on channel `{}`!", command,
                command.getClass().getSimpleName(), channelUID);
    }

    private void handleCommandForWorkType(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            findLongProperty("ep_9:sIT600TH:HoldType", "HoldType").map(DeviceProperty.LongDeviceProperty::getValue)
                    .map(value -> switch (value.intValue()) {
                        case AUTO -> "AUTO";
                        case MANUAL -> "MANUAL";
                        case TEMPORARY_MANUAL -> "TEMPORARY_MANUAL";
                        case OFF -> "OFF";
                        default -> {
                            logger.warn("Unknown value {} for property HoldType!", value);
                            yield "AUTO";
                        }
                    }).map(StringType::new).ifPresent(state -> updateState(channelUID, state));
            return;
        }

        if (command instanceof StringType typedCommand) {
            long value;
            if ("AUTO".equals(typedCommand.toString())) {
                value = AUTO;
            } else if ("MANUAL".equals(typedCommand.toString())) {
                value = MANUAL;
            } else if ("TEMPORARY_MANUAL".equals(typedCommand.toString())) {
                value = TEMPORARY_MANUAL;
            } else if ("OFF".equals(typedCommand.toString())) {
                value = OFF;
            } else {
                logger.warn("Unknown value `{}` for property HoldType!", typedCommand);
                return;
            }
            var property = findLongProperty("ep_9:sIT600TH:SetHoldType", "SetHoldType");
            if (property.isEmpty()) {
                return;
            }
            cloudApi.setValueForProperty(dsn, property.get().getName(), value);
            return;
        }

        logger.debug("Does not know how to handle command `{}` ({}) on channel `{}`!", command,
                command.getClass().getSimpleName(), channelUID);
    }

    private Optional<DeviceProperty.LongDeviceProperty> findLongProperty(String name, String shortName) {
        var deviceProperties = findDeviceProperties();
        var property = deviceProperties.stream().filter(p -> p.getName().equals(name))
                .filter(DeviceProperty.LongDeviceProperty.class::isInstance)
                .map(DeviceProperty.LongDeviceProperty.class::cast).findAny();
        if (property.isEmpty()) {
            property = deviceProperties.stream().filter(p -> p.getName().contains(shortName))
                    .filter(DeviceProperty.LongDeviceProperty.class::isInstance)
                    .map(DeviceProperty.LongDeviceProperty.class::cast).findAny();
        }
        if (property.isEmpty()) {
            logger.debug("{}/{} property not found!", name, shortName);
        }
        return property;
    }

    private SortedSet<DeviceProperty<?>> findDeviceProperties() {
        return this.cloudApi.findPropertiesForDevice(dsn);
    }
}
