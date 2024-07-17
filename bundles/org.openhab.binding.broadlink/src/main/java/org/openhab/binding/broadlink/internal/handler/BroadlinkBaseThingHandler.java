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
package org.openhab.binding.broadlink.internal.handler;

import static org.openhab.binding.broadlink.internal.BroadlinkBindingConstants.*;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.broadlink.internal.BroadlinkBindingConstants;
import org.openhab.binding.broadlink.internal.BroadlinkProtocol;
import org.openhab.binding.broadlink.internal.NetworkUtils;
import org.openhab.binding.broadlink.internal.Utils;
import org.openhab.binding.broadlink.internal.config.BroadlinkDeviceConfiguration;
import org.openhab.binding.broadlink.internal.discovery.DeviceRediscoveryAgent;
import org.openhab.binding.broadlink.internal.discovery.DeviceRediscoveryListener;
import org.openhab.binding.broadlink.internal.socket.NetworkTrafficObserver;
import org.openhab.binding.broadlink.internal.socket.RetryableSocket;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass of all supported Broadlink devices.
 *
 * @author John Marshall/Cato Sognen - Initial contribution
 */
@NonNullByDefault
public abstract class BroadlinkBaseThingHandler extends BaseThingHandler implements DeviceRediscoveryListener {

    public final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String INITIAL_DEVICE_ID = "00000000";

    protected BroadlinkDeviceConfiguration thingConfig = new BroadlinkDeviceConfiguration();

    private @Nullable RetryableSocket socket;

    // Can be injected for test purposes
    private @Nullable NetworkTrafficObserver networkTrafficObserver;

    private int count;
    private @Nullable ScheduledFuture<?> refreshHandle;
    private boolean authenticated = false;
    // These get handed to us by the device after successful authentication:
    private byte[] deviceId;
    private byte[] deviceKey;

    public BroadlinkBaseThingHandler(Thing thing) {
        super(thing);
        this.deviceId = HexUtils.hexToBytes(INITIAL_DEVICE_ID);
        this.deviceKey = HexUtils.hexToBytes(BroadlinkBindingConstants.BROADLINK_AUTH_KEY);
    }

    /**
     * Method to set the socket manually for test purposes
     *
     * @param socket the socket to use
     */
    void setSocket(RetryableSocket socket) {
        this.socket = socket;
    }

    /**
     * Method to define a networktraffic observer, who can react to the traffice being received.
     *
     * @param networkTrafficObserver
     */
    void setNetworkTrafficObserver(NetworkTrafficObserver networkTrafficObserver) {
        this.networkTrafficObserver = networkTrafficObserver;
    }

    private boolean hasAuthenticated() {
        return authenticated;
    }

    @Override
    public void initialize() {
        this.thingConfig = getConfigAs(BroadlinkDeviceConfiguration.class);
        count = (new Random()).nextInt(65535);

        if (this.socket == null) {
            this.socket = new RetryableSocket(thingConfig, logger);
        }

        updateItemStatus();

        if (thingConfig.getPollingInterval() != 0) {
            refreshHandle = scheduler.scheduleWithFixedDelay(this::updateItemStatus(), 0, thingConfig.getPollingInterval(), TimeUnit.SECONDS);
        }
    }

    @Override
    @SuppressWarnings("null")
    public void dispose() {
        if (refreshHandle != null && !refreshHandle.isDone()) {
            boolean cancelled = refreshHandle.cancel(true);
            logger.debug("Cancellation successful: {}", cancelled);
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
        super.dispose();
    }

    private boolean authenticate() {
        authenticated = false;
        // When authenticating, we must ALWAYS use the initial values
        this.deviceId = HexUtils.hexToBytes(INITIAL_DEVICE_ID);
        this.deviceKey = HexUtils.hexToBytes(BroadlinkBindingConstants.BROADLINK_AUTH_KEY);

        try {
            byte authRequest[] = buildMessage((byte) 0x65, BroadlinkProtocol.buildAuthenticationPayload(), -1);
            byte response[] = sendAndReceiveDatagram(authRequest, "authentication");
            if (response == null) {
                logger.warn(
                        "response from device during authentication was null, check if correct mac address is used for device.");
                return false;
            }
            byte decryptResponse[] = decodeDevicePacket(response);
            this.deviceId = BroadlinkProtocol.getDeviceId(decryptResponse);
            this.deviceKey = BroadlinkProtocol.getDeviceKey(decryptResponse);

            // Update the properties, so that these values can be seen in the UI:
            Map<String, String> properties = editProperties();
            properties.put("id", HexUtils.bytesToHex(deviceId));
            properties.put("key", HexUtils.bytesToHex(deviceKey));
            updateProperties(properties);
            logger.debug("Authenticated with id '{}' and key '{}'", HexUtils.bytesToHex(deviceId),
                    HexUtils.bytesToHex(deviceKey));
            authenticated = true;
            return true;
        } catch (Exception e) {
            logger.warn("Authentication failed: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("null")
    protected byte @Nullable [] sendAndReceiveDatagram(byte message[], String purpose) {
        return socket.sendAndReceive(message, purpose);
    }

    protected byte[] buildMessage(byte command, byte payload[]) throws IOException {
        return buildMessage(command, payload, thingConfig.getDeviceType());
    }

    @SuppressWarnings("null")
    private byte[] buildMessage(byte command, byte payload[], int deviceType) throws IOException {
        count = count + 1 & 0xffff;

        if (networkTrafficObserver != null) {
            networkTrafficObserver.onCommandSent(command);
            networkTrafficObserver.onBytesSent(payload);
        }

        return BroadlinkProtocol.buildMessage(command, payload, count, thingConfig.getMacAddress(), deviceId,
                HexUtils.hexToBytes(BroadlinkBindingConstants.BROADLINK_IV), deviceKey, deviceType, logger);
    }

    @SuppressWarnings("null")
    protected byte[] decodeDevicePacket(byte[] responseBytes) throws IOException {
        byte[] rxBytes = BroadlinkProtocol.decodePacket(responseBytes, this.deviceKey,
                BroadlinkBindingConstants.BROADLINK_IV);

        if (networkTrafficObserver != null) {
            networkTrafficObserver.onBytesReceived(rxBytes);
        }
        return rxBytes;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateItemStatus();
        }
    }

    // Can be implemented by devices that should do something on being found; e.g. perform a first status query
    protected boolean onBroadlinkDeviceBecomingReachable() {
        return true;
    }

    // Implemented by devices that can update the openHAB state
    // model. Return false if something went wrong that requires
    // a change in the device's online state
    protected boolean getStatusFromDevice() {
        return true;
    }

    public void updateItemStatus() {
        if ((thingConfig.getIpAddress().length() == 0) && (thingConfig.getMacAddressAsString().length() == 0)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Neither a host or static IP has been defined.");
        } else {
            try {
                if (NetworkUtils.hostAvailabilityCheck(thingConfig.getIpAddress(), 3000, logger)) {
                    if (!Utils.isOnline(getThing())) {
                        logger.trace("updateItemStatus; device not currently online, resolving");
                        transitionToOnline();
                    } else {
                        // Normal operation ...
                        boolean gotStatusOk = getStatusFromDevice();
                        if (!gotStatusOk) {
                            if (thingConfig.isIgnoreFailedUpdates()) {
                                logger.warn(
                                        "Problem getting status. Not marking offline because configured to ignore failed updates ...");
                            } else {
                                forceOffline(ThingStatusDetail.GONE, "Problem getting status");
                            }
                        }
                    }
                } else {
                    if (thingConfig.isStaticIp()) {
                        if (!Utils.isOffline(getThing())) {
                            forceOffline(ThingStatusDetail.NONE, "Couldn't find statically-IP-addressed device");
                        }
                    } else {
                        logger.debug("Dynamic IP device not found at {}, will search...", thingConfig.getIpAddress());
                        DeviceRediscoveryAgent dra = new DeviceRediscoveryAgent(thingConfig, this);
                        dra.attemptRediscovery();
                        logger.debug("Asynchronous dynamic IP device search initiated...");
                    }
                }
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot establish a communication channel with the device: " + e.getMessage());

            }
        }
    }

    @Override
    public void onDeviceRediscovered(String newIpAddress) {
        logger.debug("Rediscovered this device at IP {}", newIpAddress);
        thingConfig.setIpAddress(newIpAddress);
        transitionToOnline();
    }

    @Override
    public void onDeviceRediscoveryFailure() {
        if (!Utils.isOffline(getThing())) {
            forceOffline(ThingStatusDetail.NONE,
                    "Couldn't rediscover dynamically-IP-addressedv device after network scan");
        }
    }

    private void transitionToOnline() {
        if (!hasAuthenticated()) {
            logger.debug(
                    "We've never actually successfully authenticated with this device in this session. Doing so now");
            if (authenticate()) {
                logger.debug("Authenticated with newly-detected device, will now get its status");
            } else {
                forceOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Couldn't authenticate");
                return;
            }
        }
        if (onBroadlinkDeviceBecomingReachable()) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            forceOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Trouble getting status");
        }
    }

    @SuppressWarnings("null")
    private void forceOffline(ThingStatusDetail detail, String reason) {
        logger.warn("Online -> Offline due to: {}", reason);
        authenticated = false; // This session is dead; we'll need to re-authenticate next time
        updateStatus(ThingStatus.OFFLINE, detail, reason);
        if (socket != null) {
            socket.close();
        }
    }

    protected void updateTemperature(double temperature) {
        updateState(TEMPERATURE_CHANNEL, new QuantityType<>(temperature, BROADLINK_TEMPERATURE_UNIT));
    }

    protected void updateHumidity(double humidity) {
        updateState(HUMIDITY_CHANNEL, new QuantityType<>(humidity, BROADLINK_HUMIDITY_UNIT));
    }
}
