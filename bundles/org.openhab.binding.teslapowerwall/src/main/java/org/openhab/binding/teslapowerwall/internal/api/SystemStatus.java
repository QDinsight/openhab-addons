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
package org.openhab.binding.teslapowerwall.internal.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class for holding the set of parameters used to read the battery soe.
 *
 * @author Paul Smedley - Initial Contribution
 *
 */
public class SystemStatus {
    private static Logger LOGGER = LoggerFactory.getLogger(SystemStatus.class);

    public double full_pack_energy;

    private SystemStatus() {
    }

    public static SystemStatus parse(String response) {
        LOGGER.debug("Parsing string: \"{}\"", response);
        /* parse json string */
        JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
        SystemStatus info = new SystemStatus();
        info.full_pack_energy = jsonObject.get("nominal_full_pack_energy").getAsDouble();
        return info;
    }
}
