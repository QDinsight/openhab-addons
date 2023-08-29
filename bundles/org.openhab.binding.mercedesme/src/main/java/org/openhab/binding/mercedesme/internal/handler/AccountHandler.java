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
package org.openhab.binding.mercedesme.internal.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.mercedesme.internal.Constants;
import org.openhab.binding.mercedesme.internal.config.AccountConfiguration;
import org.openhab.binding.mercedesme.internal.proto.VehicleEvents;
import org.openhab.binding.mercedesme.internal.proto.VehicleEvents.PushMessage;
import org.openhab.binding.mercedesme.internal.server.CallbackServer;
import org.openhab.binding.mercedesme.internal.utils.AuthService;
import org.openhab.binding.mercedesme.internal.utils.Utils;
import org.openhab.core.auth.client.oauth2.AccessTokenRefreshListener;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AccountHandler} takes care of the valid authorization for the user account
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
@WebSocket
public class AccountHandler extends BaseBridgeHandler implements AccessTokenRefreshListener {
    private final Logger logger = LoggerFactory.getLogger(AccountHandler.class);
    private final HttpClient httpClient;
    private final LocaleProvider localeProvider;
    private final Storage<AccessTokenResponse> storage;
    private Optional<CallbackServer> server = Optional.empty();
    private Optional<WebSocketClient> wsClient = Optional.empty();
    private Optional<AuthService> authService = Optional.empty();
    @Nullable
    private Session session = null;
    private boolean connected = false;

    Optional<AccountConfiguration> config = Optional.empty();

    public AccountHandler(Bridge bridge, HttpClient hc, LocaleProvider lp, StorageService store) {
        super(bridge);
        httpClient = hc;
        localeProvider = lp;
        storage = store.getStorage(Constants.BINDING_ID);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // no commands available
    }

    @Override
    public void initialize() {
        config = Optional.of(getConfigAs(AccountConfiguration.class));
        autodetectCallback();
        String configValidReason = configValid();
        if (!configValidReason.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, configValidReason);
        } else {
            String callbackUrl = Utils.getCallbackAddress(config.get().callbackIP, config.get().callbackPort);
            thing.setProperty("callbackUrl", callbackUrl);
            server = Optional.of(new CallbackServer(this, httpClient, config.get(), callbackUrl, localeProvider));
            authService = Optional.of(new AuthService(httpClient, config.get(), localeProvider.getLocale(), storage));
            if (!server.get().start()) {
                String textKey = Constants.STATUS_TEXT_PREFIX + thing.getThingTypeUID().getId()
                        + Constants.STATUS_SERVER_RESTART;
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, textKey);
            }
        }
        scheduler.schedule(this::startWebsocket, 1, TimeUnit.SECONDS);
    }

    private void startWebsocket() {
        if (connected) {
            logger.info("WS already connected");
            return;
        }
        wsClient = Optional.of(new WebSocketClient());
        try {
            wsClient.get().start();
            URI echoUri = new URI(Utils.getWebsocketServer(config.get().region));
            ClientUpgradeRequest request = new ClientUpgradeRequest();

            request.setHeader("Authorization", authService.get().getToken());
            request.setHeader("X-SessionId", UUID.randomUUID().toString());
            request.setHeader("X-TrackingId", UUID.randomUUID().toString());
            request.setHeader("Ris-Os-Name", Constants.RIS_OS_NAME);
            request.setHeader("Ris-Os-Version", Constants.RIS_OS_VERSION);
            request.setHeader("Ris-Sdk-Version", Utils.getRisSDKVersion(config.get().region));
            request.setHeader("X-Locale",
                    localeProvider.getLocale().getLanguage() + "-" + localeProvider.getLocale().getCountry()); // de-DE
            request.setHeader("User-Agent", Utils.getApplication(config.get().region));
            request.setHeader("X-Applicationname", Utils.getUserAgent(config.get().region));
            request.setHeader("Ris-Application-Version", Utils.getRisApplicationVersion(config.get().region));
            logger.info("Connect WS");
            wsClient.get().connect(this, echoUri, request);
            connected = true;
        } catch (Throwable t) {
            logger.warn("Connection exception {}", t.getMessage());
            connected = false;
        }
        logger.info("WS start finished");
    }

    private void autodetectCallback() {
        // if Callback IP and Callback Port are not set => autodetect these values
        config = Optional.of(getConfigAs(AccountConfiguration.class));
        Configuration updateConfig = super.editConfiguration();
        if (!updateConfig.containsKey("callbackPort")) {
            updateConfig.put("callbackPort", Utils.getFreePort());
        } else {
            Utils.addPort(config.get().callbackPort);
        }
        if (!updateConfig.containsKey("callbackIP")) {
            String ip;
            try {
                ip = Utils.getCallbackIP();
                updateConfig.put("callbackIP", ip);
            } catch (SocketException e) {
                logger.info("Cannot detect IP address {}", e.getMessage());
            }
        }
        super.updateConfiguration(updateConfig);
        // get new config after update
        config = Optional.of(getConfigAs(AccountConfiguration.class));
    }

    private String configValid() {
        config = Optional.of(getConfigAs(AccountConfiguration.class));
        String textKey = Constants.STATUS_TEXT_PREFIX + thing.getThingTypeUID().getId();
        if (config.get().callbackIP.equals(Constants.NOT_SET)) {
            return textKey + Constants.STATUS_IP_MISSING;
        } else if (config.get().callbackPort == -1) {
            return textKey + Constants.STATUS_PORT_MISSING;
        } else {
            return Constants.EMPTY;
        }
    }

    @Override
    public void dispose() {
        if (server.isPresent()) {
            CallbackServer serv = server.get();
            serv.stop();
            serv.dispose();
            server = Optional.empty();
            Utils.removePort(config.get().callbackPort);
        }
    }

    @Override
    public void handleRemoval() {
        server.ifPresent(s -> s.deleteOAuthServiceAndAccessToken());
        wsClient.ifPresent(c -> c.destroy());
        super.handleRemoval();
    }

    /**
     * https://next.openhab.org/javadoc/latest/org/openhab/core/auth/client/oauth2/package-summary.html
     */
    @Override
    public void onAccessTokenResponse(AccessTokenResponse tokenResponse) {
        if (!tokenResponse.getAccessToken().isEmpty()) {
            // token not empty - fine
            updateStatus(ThingStatus.ONLINE);
            scheduler.schedule(this::startWebsocket, 1, TimeUnit.SECONDS);
        } else if (server.isEmpty()) {
            // server not running - fix first
            String textKey = Constants.STATUS_TEXT_PREFIX + thing.getThingTypeUID().getId()
                    + Constants.STATUS_SERVER_RESTART;
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, textKey);
        } else {
            // all failed - start manual authorization
            String textKey = Constants.STATUS_TEXT_PREFIX + thing.getThingTypeUID().getId()
                    + Constants.STATUS_AUTH_NEEDED;
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE,
                    textKey + " [\"" + thing.getProperties().get("callbackUrl") + "\"]");
        }
    }

    @Override
    public String toString() {
        return Integer.toString(config.get().callbackPort);
    }

    /**
     * Websocket endpoint
     */

    @OnWebSocketMessage
    public void onBytes(InputStream is) {
        // public void onBytes(byte buf[], int offset, int length) {
        try {
            PushMessage pm = VehicleEvents.PushMessage.parseFrom(is);
            Map m = pm.getAllFields();
            // FieldDescriptor fd = new FieldDescriptor()
            Set keys = m.keySet();
            for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                Object object = iterator.next();
                logger.info("{}", object);
            }
        } catch (IOException e) {
            logger.warn("Error parsing message {}", e.getMessage());
        }
    }

    @OnWebSocketClose
    public void onDisconnect(Session session, int statusCode, String reason) {
        logger.info("Disonnected from server. Status {} Reason {}", statusCode, reason);
        connected = false;
        scheduler.schedule(this::startWebsocket, 1, TimeUnit.SECONDS);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Connected to server");
        connected = true;
        this.session = session;
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        logger.warn("Error {}", t.getMessage());
        // t.printStackTrace();
    }
}
