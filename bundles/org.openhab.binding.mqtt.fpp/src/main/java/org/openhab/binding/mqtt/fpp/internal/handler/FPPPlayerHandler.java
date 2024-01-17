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
package org.openhab.binding.mqtt.fpp.internal.handler;

import static org.openhab.binding.mqtt.fpp.internal.FPPBindingConstants.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.fpp.internal.ConfigOptions;
import org.openhab.binding.mqtt.fpp.internal.FPPStatus;
import org.openhab.binding.mqtt.handler.AbstractBrokerHandler;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import tech.units.indriya.unit.Units;

/**
 * The {@link FPPPlayerHandler} is responsible for handling commands of the globes, which are then
 * sent to one of the bridges to be sent out by MQTT.
 *
 * @author Scott Hanson - Initial contribution
 */
@NonNullByDefault
public class FPPPlayerHandler extends BaseThingHandler implements MqttMessageSubscriber {
    // these are all constants used in color conversion calcuations.
    // strings are necessary to prevent floating point loss of precision

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private @Nullable MqttBrokerConnection connection;
    private ThingRegistry thingRegistry;

    private String fullStatusTopic = "";
    private String fullVersionTopic = "";
    private String fullCommandTopic = "";
    private int lastKnownVolume = 50;
    private ConfigOptions config = new ConfigOptions();

    private final Gson gson = new Gson();

    public FPPPlayerHandler(Thing thing, ThingRegistry thingRegistry) {
        super(thing);
        this.thingRegistry = thingRegistry;
    }

    private void processIncomingState(String messageJSON) {
        // Need to handle State and Level at the same time to process level=0 as off//
        FPPStatus data = gson.fromJson(messageJSON, FPPStatus.class);
        updateState(CHANNEL_STATUS, new StringType(data.status_name));
        updateState(CHANNEL_PLAYER, data.status == 1 ? PlayPauseType.PLAY : PlayPauseType.PAUSE);
        lastKnownVolume = data.volume;
        updateState(CHANNEL_VOLUME, new PercentType(data.volume));
        updateState(CHANNEL_MODE, new StringType(data.mode_name));
        updateState(CHANNEL_CURRENT_SEQUENCE, new StringType(data.current_sequence));
        updateState(CHANNEL_CURRENT_SONG, new StringType(data.current_song));
        updateState(CHANNEL_CURRENT_PLAYLIST, new StringType(data.current_playlist.playlist));
        updateState(CHANNEL_UUID, new StringType(data.uuid));
        updateState(CHANNEL_SEC_PLAYED, new QuantityType<>(new BigDecimal(data.seconds_played), Units.SECOND));
        updateState(CHANNEL_SEC_REMAINING, new QuantityType<>(new BigDecimal(data.seconds_remaining), Units.SECOND));
        updateState(CHANNEL_UPTIME, new QuantityType<>(new BigDecimal(data.uptimeTotalSeconds), Units.SECOND));
    }

    void handleLevelColour(Command command) {
        if (command instanceof OnOffType) {

        } else if (command instanceof IncreaseDecreaseType) {

            // sendMQTT("{\"state\":\"ON\",\"level\":" + savedLevel.intValue() + "}");
            return;
        } else if (command instanceof PercentType percentType) {
            // sendMQTT("{\"state\":\"ON\",\"level\":" + command + "}");
            return;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            return;
        }
        String channelId = channelUID.getId();
        if (channelId.equals(CHANNEL_PLAYER)) {
            if (command == PlayPauseType.PAUSE || command == OnOffType.OFF) {
                executeGet("/api/playlists/stop");
            } else if (command == PlayPauseType.PLAY || command == OnOffType.ON) {

            } else if (command == NextPreviousType.NEXT) {
                executeGet("/api/command/Next Playlist Item");
            } else if (command == NextPreviousType.PREVIOUS) {
                executeGet("/api/command/Prev Playlist Item");
            }
        }
        if (channelId.equals(CHANNEL_VOLUME)) {
            Integer volume = null;
            if (command instanceof PercentType percentCommand) {
                volume = percentCommand.intValue();
            } else if (command == OnOffType.OFF) {
                volume = 0;
            } else if (command == OnOffType.ON) {
                volume = lastKnownVolume;
            } else if (command == IncreaseDecreaseType.INCREASE) {
                if (lastKnownVolume < 100) {
                    lastKnownVolume++;
                    volume = lastKnownVolume;
                }
            } else if (command == IncreaseDecreaseType.DECREASE) {
                if (lastKnownVolume > 0) {
                    lastKnownVolume--;
                    volume = lastKnownVolume;
                }
            }
            if (volume != null) {
                lastKnownVolume = volume;
                // { "volume": 34 }
                executePost("/api/system/volume", "{\"volume\":" + lastKnownVolume + "}");
                updateState(CHANNEL_VOLUME, new PercentType(lastKnownVolume));
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(ConfigOptions.class);

        fullStatusTopic = config.playerMQTT + "/" + STATUS_TOPIC;
        fullVersionTopic = config.playerMQTT + "/" + VERSION_TOPIC;
        // Need to remove the lowercase x from 0x12AB in case it contains all numbers

        bridgeStatusChanged(getBridgeStatus());
    }

    @Override
    public void processMessage(String topic, byte[] payload) {
        String state = new String(payload, StandardCharsets.UTF_8);
        logger.trace("Received the following new FPP state:{}:{}", topic, state);

        if (topic.endsWith(STATUS_TOPIC)) {
            processIncomingState(state);
        } else if (topic.endsWith(VERSION_TOPIC)) {
            updateState(CHANNEL_VERSION, new StringType(state));
            updateStatus(ThingStatus.ONLINE);
        }
    }

    public ThingStatusInfo getBridgeStatus() {
        Bridge b = getBridge();
        if (b != null) {
            return b.getStatusInfo();
        } else {
            return new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, null);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            connection = null;
            return;
        }
        if (bridgeStatusInfo.getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            return;
        }

        Bridge localBridge = this.getBridge();
        if (localBridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                    "Bridge is missing or offline, you need to setup a working MQTT broker first.");
            return;
        }
        ThingHandler handler = localBridge.getHandler();
        if (handler instanceof AbstractBrokerHandler abh) {
            final MqttBrokerConnection connection;
            try {
                connection = abh.getConnectionAsync().get(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                        "Bridge handler has no valid broker connection!");
                return;
            }
            this.connection = connection;
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Waiting for 'fpp/status: connected' MQTT message to be received. Check hub has 'MQTT Client Status Topic' configured.");
            connection.subscribe(fullStatusTopic, this);
            connection.subscribe(fullVersionTopic, this);
        }
        return;
    }

    private @Nullable String executeGet(String url) {
        String response = null;
        try {
            response = HttpUtil.executeUrl("GET", "http://" + config.playerIP + url, 5000);

        } catch (Exception e) {
            logger.error("Failed HTTP Post", e);
        }
        return response;
    }

    private boolean executePost(String url, String json) {
        try {
            Properties header = new Properties();
            header.put("Accept", "application/json");
            // header.put("Connection", "keep-alive");
            header.put("Content-Type", "application/json");
            String response = HttpUtil.executeUrl("POST", "http://" + config.playerIP + url, header,
                    new ByteArrayInputStream(json.getBytes()), "application/json", 5000);

            return !response.isEmpty();
        } catch (Exception e) {
            logger.error("Failed HTTP Post", e);
        }
        return false;
    }

    @Override
    public void dispose() {
        MqttBrokerConnection localConnection = connection;
        if (localConnection != null) {
            localConnection.unsubscribe(fullStatusTopic, this);
            localConnection.unsubscribe(fullVersionTopic, this);
        }
    }
}
