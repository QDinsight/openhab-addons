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
package org.openhab.binding.siemenshvac.internal.network;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.siemenshvac.internal.handler.SiemensHvacBridgeBaseThingHandler;
import org.openhab.binding.siemenshvac.internal.handler.SiemensHvacBridgeConfig;

import com.google.gson.JsonObject;

/**
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public interface SiemensHvacConnector {

    @Nullable
    String DoBasicRequest(String uri) throws Exception;

    @Nullable
    JsonObject doRequest(String req, @Nullable SiemensHvacCallback callback);

    public void WaitAllPendingRequest();

    public void WaitNoNewRequest();

    public void onComplete(Request request);

    public void onError(Request request, SiemensHvacCallback cb) throws Exception;

    public void setSiemensHvacBridgeBaseThingHandler(SiemensHvacBridgeBaseThingHandler hvacBridgeBaseThingHandler);

    public @Nullable SiemensHvacBridgeConfig getBridgeConfiguration();

    void ResetSessionId(boolean web);
}
