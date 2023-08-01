/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.androidtv.internal.protocol.philipstv.service;

import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVBindingConstants.CHANNEL_MUTE;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVBindingConstants.CHANNEL_VOLUME;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVBindingConstants.KEY_CODE_PATH;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVBindingConstants.TV_NOT_LISTENING_MSG;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVBindingConstants.TV_OFFLINE_MSG;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVBindingConstants.VOLUME_PATH;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVConnectionManager.OBJECT_MAPPER;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.service.KeyCode.KEY_MUTE;

import java.io.IOException;

import org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVConnectionManager;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.api.PhilipsTVService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.model.keycode.KeyCodeDto;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.model.volume.VolumeDto;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VolumeService} is responsible for handling volume commands, which are sent to the
 * volume channel or mute channel.
 *
 * @author Benjamin Meyer - Initial contribution
 */
public class VolumeService implements PhilipsTVService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PhilipsTVConnectionManager connectionManager;

    public VolumeService(PhilipsTVConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void handleCommand(String channel, Command command) {
        try {
            if (command instanceof RefreshType) {
                VolumeDto volumeDto = getVolume();
                connectionManager.postUpdateChannel(CHANNEL_VOLUME, new PercentType(volumeDto.getCurrentVolume()));
                connectionManager.postUpdateChannel(CHANNEL_MUTE, volumeDto.isMuted() ? OnOffType.ON : OnOffType.OFF);
            } else if (CHANNEL_VOLUME.equals(channel) && command instanceof PercentType) {
                setVolume((PercentType) command);
                connectionManager.postUpdateChannel(CHANNEL_VOLUME, (PercentType) command);
            } else if (CHANNEL_MUTE.equals(channel) && command instanceof OnOffType) {
                setMute();
            } else {
                logger.warn("Unknown command: {} for Channel {}", command, channel);
            }
        } catch (Exception e) {
            if (isTvOfflineException(e)) {
                connectionManager.postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.NONE, TV_OFFLINE_MSG);
            } else if (isTvNotListeningException(e)) {
                connectionManager.postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        TV_NOT_LISTENING_MSG);
            } else {
                logger.warn("Error during handling the VolumeService command {} for Channel {}: {}", command, channel,
                        e.getMessage(), e);
            }
        }
    }

    private VolumeDto getVolume() throws IOException {
        String jsonContent = connectionManager.doHttpsGet(VOLUME_PATH);
        return OBJECT_MAPPER.readValue(jsonContent, VolumeDto.class);
    }

    private void setVolume(PercentType volumeToSet) throws IOException {
        VolumeDto volumeDto = new VolumeDto();
        volumeDto.setMuted(false);
        volumeDto.setCurrentVolume(volumeToSet.intValue());
        String volumeJson = OBJECT_MAPPER.writeValueAsString(volumeDto);
        logger.debug("Set json volume: {}", volumeJson);
        connectionManager.doHttpsPost(VOLUME_PATH, volumeJson);
    }

    private void setMute() throws IOException {
        // We just sent the KEY_MUTE and dont bother what was actually requested
        KeyCodeDto keyCodeDto = new KeyCodeDto();
        keyCodeDto.setKey(KEY_MUTE);
        String muteJson = OBJECT_MAPPER.writeValueAsString(keyCodeDto);
        logger.debug("Set json mute state: {}", muteJson);
        connectionManager.doHttpsPost(KEY_CODE_PATH, muteJson);
    }
}
