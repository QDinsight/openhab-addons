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
package org.openhab.binding.androidtv.internal.protocol.philipstv;

import static org.openhab.binding.androidtv.internal.AndroidTVBindingConstants.*;
import static org.openhab.binding.androidtv.internal.protocol.philipstv.PhilipsTVBindingConstants.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.androidtv.internal.AndroidTVDynamicStateDescriptionProvider;
import org.openhab.binding.androidtv.internal.AndroidTVHandler;
import org.openhab.binding.androidtv.internal.AndroidTVTranslationProvider;
import org.openhab.binding.androidtv.internal.protocol.philipstv.pairing.PhilipsTVPairing;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.AmbilightService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.AppService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.KeyPressService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.PowerService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.SearchContentService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.TvChannelService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.TvPictureService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.VolumeService;
import org.openhab.binding.androidtv.internal.protocol.philipstv.service.api.PhilipsTVService;
import org.openhab.core.OpenHAB;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link PhilipsTVHandler} is responsible for handling commands, which are sent to one of the
 * channels.
 *
 * @author Benjamin Meyer - Initial contribution
 * @author Ben Rosenblum - Merged into AndroidTV
 */
@NonNullByDefault
public class PhilipsTVConnectionManager implements DiscoveryListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AndroidTVHandler handler;

    public PhilipsTVConfiguration config;

    private ScheduledExecutorService scheduler;

    private final AndroidTVTranslationProvider translationProvider;

    private DiscoveryServiceRegistry discoveryServiceRegistry;

    private @Nullable AndroidTVDynamicStateDescriptionProvider stateDescriptionProvider;

    private @Nullable ThingUID upnpThingUID;

    private @Nullable ScheduledFuture<?> refreshScheduler;

    private final Predicate<ScheduledFuture<?>> isRefreshSchedulerRunning = r -> (r != null) && !r.isCancelled();

    private final ReentrantLock lock = new ReentrantLock();

    private boolean isLoggedIn = false;

    private String statusMessage = "";

    private String username = "";
    private String password = "";
    private String macAddress = "";

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /* Philips TV services */
    private @Nullable Map<String, PhilipsTVService> channelServices;

    public PhilipsTVConnectionManager(AndroidTVHandler handler, PhilipsTVConfiguration config) {

        logger.debug("Create a Philips TV Handler for thing '{}'", handler.getThingUID());
        this.handler = handler;
        this.config = config;
        this.scheduler = handler.getScheduler();
        this.translationProvider = handler.getTranslationProvider();
        this.discoveryServiceRegistry = handler.getDiscoveryServiceRegistry();
        this.stateDescriptionProvider = handler.getStateDescriptionProvider();
        initialize();
    }

    private void setStatus(boolean isLoggedIn) {
        if (isLoggedIn) {
            setStatus(isLoggedIn, "online.online");
        } else {
            setStatus(isLoggedIn, "offline.unknown");
        }
    }

    private void setStatus(boolean isLoggedIn, String statusMessage) {
        String translatedMessage = translationProvider.getText(statusMessage);
        if ((this.isLoggedIn != isLoggedIn) || (!this.statusMessage.equals(translatedMessage))) {
            this.isLoggedIn = isLoggedIn;
            this.statusMessage = translatedMessage;
            handler.checkThingStatus();
        }
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        if (this.isLoggedIn != isLoggedIn) {
            setStatus(isLoggedIn);
        }
    }

    public boolean getLoggedIn() {
        return isLoggedIn;
    }

    public void updateStatus(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail, String thingStatusMessage) {
        if (thingStatus == ThingStatus.ONLINE) {
            setLoggedIn(true);
        } else {
            setStatus(false, thingStatusMessage);
        }
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public void saveConfigs() {
        String folderName = OpenHAB.getUserDataFolder() + "/androidtv";
        File folder = new File(folderName);

        if (!folder.exists()) {
            logger.debug("Creating directory {}", folderName);
            folder.mkdirs();
        }

        String fileName = folderName + "/philipstv." + handler.getThing().getUID().getId() + ".config";
        File configFile = new File(fileName);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("username", username);
        configMap.put("password", password);
        configMap.put("macAddress", macAddress);

        try {
            String configJson = OBJECT_MAPPER.writeValueAsString(configMap);
            logger.debug("Writing configJson \"{}\" to {}", configJson, fileName);
            Files.write(Paths.get(fileName), configJson.getBytes());
        } catch (JsonProcessingException e) {
            logger.warn("JsonProcessingException trying to save configMap: {}", e.getMessage(), e);
        } catch (IOException ex) {
            logger.debug("IOException when writing configJson to file {}", ex.getMessage());
        }
    }

    private void readConfigs() {
        String folderName = OpenHAB.getUserDataFolder() + "/androidtv";
        String fileName = folderName + "/philipstv." + handler.getThing().getUID().getId() + ".config";
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }
        try {
            final byte[] contents = Files.readAllBytes(Paths.get(fileName));
            String configJson = new String(contents);
            logger.debug("Read configJson \"{}\" from {}", configJson, fileName);
            Map<String, String> configMap = OBJECT_MAPPER.readValue(configJson,
                    new TypeReference<HashMap<String, String>>() {
                    });
            this.username = configMap.get("username").toString();
            this.password = configMap.get("password").toString();
            this.macAddress = configMap.get("macAddress").toString();
            logger.debug("Processed configJson as {} {} {}", this.username, this.password, this.macAddress);
        } catch (IOException ex) {
            logger.debug("IOException when reading configJson from file {}", ex.getMessage());
        }
    }

    public void setCreds(String username, String password) {
        this.username = username;
        this.password = password;
        saveConfigs();
    }

    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received channel: {}, command: {}", channelUID, command);
        String username = this.username;
        String password = this.password;

        if (channelUID.getId().equals(CHANNEL_PINCODE)) {
            if (command instanceof StringType) {
                HttpHost target = new HttpHost(config.ipAddress, config.port, HTTPS);
                if (command.toString().equals("REQUEST")) {
                    try {
                        initPairingCodeRetrieval(target);
                    } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                "Error occurred while trying to present a Pairing Code on TV.");
                    }
                } else {
                    boolean hasFailed = initCredentialsRetrieval(target, command.toString());
                    if (hasFailed) {
                        postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                "Error occurred during retrieval of credentials.");
                    }
                }
            }
            return;
        }

        if ((username.isEmpty()) || (password.isEmpty())) {
            return; // pairing process is not finished
        }

        if ((!isLoggedIn) && (!channelUID.getId().equals(CHANNEL_POWER)
                & !channelUID.getId().equals(CHANNEL_AMBILIGHT_LOUNGE_POWER))) {
            // Check if tv turned on meanwhile
            channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
            if (!isLoggedIn) {
                // still offline
                logger.info(
                        "Cannot execute command {} for channel {}: PowerState of TV was checked and resolved to offline.",
                        command, channelUID.getId());
                return;
            }
        }

        String channel = channelUID.getId();
        long startTime = System.currentTimeMillis();
        // Delegate the other commands to correct channel service
        PhilipsTVService philipsTvService = channelServices.get(channel);

        if (philipsTvService == null) {
            logger.warn("Unknown channel for Philips TV Binding: {}", channel);
            return;
        }

        philipsTvService.handleCommand(channel, command);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        logger.trace("The command {} took : {} nanoseconds", command.toFullString(), elapsedTime);
    }

    public void initialize() {
        logger.debug("Init of handler for Thing: {}", handler.getThingID());

        if ((config.ipAddress == null) || (config.port == null)) {
            postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Cannot connect to Philips TV. Host and/or port are not set.");
            return;
        }

        HttpHost target = new HttpHost(config.ipAddress, config.port, HTTPS);
        readConfigs();
        String username = this.username;
        String password = this.password;
        String macAddress = this.macAddress;

        if ((username.isEmpty()) || (password.isEmpty())) {
            if (config.pairingCode.isEmpty()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                        "Pairing is not configured yet, trying to present a Pairing Code on TV.");
                try {
                    initPairingCodeRetrieval(target);
                } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Error occurred while trying to present a Pairing Code on TV.");
                }
            } else if (!config.pairingCode.isEmpty()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                        "Pairing Code is available, but credentials missing. Trying to retrieve them.");
                boolean hasFailed = initCredentialsRetrieval(target);
                if (hasFailed) {
                    postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Error occurred during retrieval of credentials.");
                }
            }
            return;
        }

        if (!config.useUpnpDiscovery && isSchedulerInitializable()) {
            startRefreshScheduler();
        }

        CloseableHttpClient httpClient;

        try {
            httpClient = ConnectionManagerUtil.createSharedHttpClient(target, username, password);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    String.format("Error occurred during creation of HTTP client: %s", e.getMessage()));
            return;
        }

        ConnectionManager connectionManager = new ConnectionManager(httpClient, target);

        if (macAddress.isEmpty()) {
            try {
                Optional<String> wolAddress = WakeOnLanUtil.getMacFromEnabledInterface(connectionManager);
                if (wolAddress.isPresent()) {
                    this.macAddress = wolAddress.get();
                    saveConfigs();
                } else {
                    logger.debug("MAC Address could not be determined for Wake-On-LAN support, "
                            + "because Wake-On-LAN is not enabled on the TV.");
                }
            } catch (IOException e) {
                logger.debug("Error occurred during retrieval of MAC Address: {}", e.getMessage());
            }
        }

        Map<String, PhilipsTVService> services = new HashMap<>();

        PhilipsTVService volumeService = new VolumeService(this, connectionManager);
        services.put(CHANNEL_VOLUME, volumeService);
        services.put(CHANNEL_MUTE, volumeService);

        PhilipsTVService tvPictureService = new TvPictureService(this, connectionManager);
        services.put(CHANNEL_BRIGHTNESS, tvPictureService);
        services.put(CHANNEL_SHARPNESS, tvPictureService);
        services.put(CHANNEL_CONTRAST, tvPictureService);

        PhilipsTVService keyPressService = new KeyPressService(this, connectionManager);
        services.put(CHANNEL_KEYPRESS, keyPressService);
        services.put(CHANNEL_PLAYER, keyPressService);

        PhilipsTVService appService = new AppService(this, connectionManager);
        services.put(CHANNEL_APPNAME, appService);
        services.put(CHANNEL_APP_ICON, appService);

        PhilipsTVService ambilightService = new AmbilightService(this, connectionManager);
        services.put(CHANNEL_AMBILIGHT_POWER, ambilightService);
        services.put(CHANNEL_AMBILIGHT_HUE_POWER, ambilightService);
        services.put(CHANNEL_AMBILIGHT_LOUNGE_POWER, ambilightService);
        services.put(CHANNEL_AMBILIGHT_STYLE, ambilightService);
        services.put(CHANNEL_AMBILIGHT_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_LEFT_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_RIGHT_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_TOP_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_BOTTOM_COLOR, ambilightService);

        services.put(CHANNEL_TV_CHANNEL, new TvChannelService(this, connectionManager));
        services.put(CHANNEL_POWER, new PowerService(this, connectionManager));
        services.put(CHANNEL_SEARCH_CONTENT, new SearchContentService(this, connectionManager));
        channelServices = Collections.unmodifiableMap(services);

        if (discoveryServiceRegistry != null) {
            discoveryServiceRegistry.addDiscoveryListener(this);
        }

        // Thing is initialized, check power state and available communication of the TV and set ONLINE or OFFLINE
        postUpdateThing(ThingStatus.ONLINE, ThingStatusDetail.NONE, EMPTY);
        channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
    }

    /**
     * Starts the pairing Process with the TV, which results in a Pairing Code shown on TV.
     */
    private void initPairingCodeRetrieval(HttpHost target)
            throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        logger.info("Pairing code for tv authentication is missing. "
                + "Starting initial pairing process. Please provide manually the pairing code shown on the tv at the configuration of the tv thing.");
        PhilipsTVPairing pairing = new PhilipsTVPairing();
        pairing.requestPairingPin(target);
    }

    private boolean initCredentialsRetrieval(HttpHost target) {
        return initCredentialsRetrieval(target, "");
    }

    private boolean initCredentialsRetrieval(HttpHost target, String pincode) {
        boolean hasFailed = false;
        logger.info(
                "Pairing code is available, but username and/or password is missing. Therefore we try to grant authorization and retrieve username and password.");
        PhilipsTVPairing pairing = new PhilipsTVPairing();
        try {
            if (pincode.isEmpty()) {
                pairing.finishPairingWithTv(config, this, target);
            } else {
                pairing.finishPairingWithTv(pincode, this, target);
            }
            postUpdateThing(ThingStatus.ONLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Authentication with Philips TV device was successful. Continuing initialization of the tv.");
        } catch (Exception e) {
            postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                    "Could not successfully finish pairing process with the TV.");
            logger.warn("Error during finishing pairing process with the TV: {}", e.getMessage(), e);
            hasFailed = true;
        }
        return hasFailed;
    }

    // callback methods for channel services
    public void postUpdateChannel(String channelUID, State state) {
        handler.updateChannelState(channelUID, state);
    }

    public synchronized void postUpdateThing(ThingStatus status, ThingStatusDetail statusDetail, String msg) {
        if (status == ThingStatus.ONLINE) {
            if (msg.equalsIgnoreCase(STANDBY)) {
                handler.updateChannelState(CHANNEL_POWER, OnOffType.OFF);
            } else {
                handler.updateChannelState(CHANNEL_POWER, OnOffType.ON);
            }
            if (isSchedulerInitializable()) { // Init refresh scheduler only, if pairing is completed
                startRefreshScheduler();
            }
        } else if (status == ThingStatus.OFFLINE) {
            handler.updateChannelState(CHANNEL_POWER, OnOffType.OFF);
            if (!TV_NOT_LISTENING_MSG.equals(msg)) { // avoid cancelling refresh if TV is temporarily not available
                ScheduledFuture<?> refreshScheduler = this.refreshScheduler;
                if (refreshScheduler != null) {
                    if (config.useUpnpDiscovery && isRefreshSchedulerRunning.test(refreshScheduler)) {
                        stopRefreshScheduler();
                    }
                }
                // Reset app and channel list (if existing) for new retrieval during next startup
                if (channelServices != null) {
                    ((AppService) channelServices.get(CHANNEL_APPNAME)).clearAvailableAppList();
                    ((TvChannelService) channelServices.get(CHANNEL_TV_CHANNEL)).clearAvailableTvChannelList();
                }
            }
        }
        updateStatus(status, statusDetail, msg);
    }

    private boolean isSchedulerInitializable() {
        String username = this.username;
        String password = this.password;
        return (!username.isEmpty()) && (!password.isEmpty())
                && ((refreshScheduler == null) || refreshScheduler.isDone());
    }

    private void startRefreshScheduler() {
        int configuredRefreshRateOrDefault = Optional.ofNullable(config.refreshRate).orElse(10);
        if (configuredRefreshRateOrDefault > 0) { // If value equals zero, refreshing should not be scheduled
            logger.info("Starting Refresh Scheduler for Philips TV {} with refresh rate of {}.", handler.getThingID(),
                    configuredRefreshRateOrDefault);
            refreshScheduler = scheduler.scheduleWithFixedDelay(this::refreshTvProperties, 10,
                    configuredRefreshRateOrDefault, TimeUnit.SECONDS);
        }
    }

    private void stopRefreshScheduler() {
        logger.info("Stopping Refresh Scheduler for Philips TV: {}", handler.getThingID());
        refreshScheduler.cancel(true);
    }

    private void refreshTvProperties() {
        try {
            boolean isLockAcquired = lock.tryLock(1, TimeUnit.SECONDS);
            if (isLockAcquired) {
                try {
                    if (!isLoggedIn || !config.useUpnpDiscovery) {
                        channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
                        if (!isLoggedIn) {
                            return;
                        }
                    }
                    channelServices.get(CHANNEL_VOLUME).handleCommand(CHANNEL_VOLUME, RefreshType.REFRESH);
                    channelServices.get(CHANNEL_APPNAME).handleCommand(CHANNEL_APPNAME, RefreshType.REFRESH);
                    channelServices.get(CHANNEL_TV_CHANNEL).handleCommand(CHANNEL_TV_CHANNEL, RefreshType.REFRESH);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Exception occurred during refreshing the tv properties: {}", e.getMessage());
        }
    }

    public void updateChannelStateDescription(final String channelId, Map<String, String> values) {
        List<StateOption> options = new ArrayList<>();
        values.forEach((key, value) -> options.add(new StateOption(key, value)));
        stateDescriptionProvider.setStateOptions(new ChannelUID(handler.getThingUID(), channelId), options);
    }

    @Override
    public void thingDiscovered(DiscoveryService source, DiscoveryResult result) {
        logger.debug("thingDiscovered: {}", result);

        if (config.useUpnpDiscovery && config.ipAddress.equals(result.getProperties().get(HOST))) {
            upnpThingUID = result.getThingUID();
            logger.debug("thingDiscovered, thingUID={}, discoveredUID={}", handler.getThingUID(), upnpThingUID);
            channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
        }
    }

    @Override
    public void thingRemoved(DiscoveryService discoveryService, ThingUID thingUID) {
        logger.debug("thingRemoved: {}", thingUID);

        if (thingUID.equals(upnpThingUID)) {
            postUpdateThing(ThingStatus.ONLINE, ThingStatusDetail.NONE, STANDBY);
        }
    }

    @Override
    public @Nullable Collection<ThingUID> removeOlderResults(DiscoveryService discoveryService, long l,
            @Nullable Collection<ThingTypeUID> collection, @Nullable ThingUID thingUID) {
        return Collections.emptyList();
    }

    public void dispose() {
        if (discoveryServiceRegistry != null) {
            discoveryServiceRegistry.removeDiscoveryListener(this);
        }
        ScheduledFuture<?> refreshScheduler = this.refreshScheduler;
        if (refreshScheduler != null) {
            if (isRefreshSchedulerRunning.test(refreshScheduler)) {
                stopRefreshScheduler();
            }
        }
    }
}
