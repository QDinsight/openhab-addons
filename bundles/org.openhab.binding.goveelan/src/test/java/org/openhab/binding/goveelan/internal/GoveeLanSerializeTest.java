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
package org.openhab.binding.goveelan.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.goveelan.internal.model.Color;
import org.openhab.binding.goveelan.internal.model.ColorData;
import org.openhab.binding.goveelan.internal.model.EmptyValueQueryStatusData;
import org.openhab.binding.goveelan.internal.model.GenericGoveeMessage;
import org.openhab.binding.goveelan.internal.model.GenericGoveeMsg;
import org.openhab.binding.goveelan.internal.model.ValueData;

import com.google.gson.Gson;

/**
 * @author Stefan Höhn - Initial contribution
 */
@NonNullByDefault
public class GoveeLanSerializeTest {

    private static final Gson GSON = new Gson();
    private final String lightOffJsonString = "{\"msg\":{\"cmd\":\"turn\",\"data\":{\"value\":0}}}";
    private final String lightOnJsonString = "{\"msg\":{\"cmd\":\"brightness\",\"data\":{\"value\":100}}}";
    private final String lightColorJsonString = "{\"msg\":{\"cmd\":\"colorwc\",\"data\":{\"color\":{\"r\":0,\"g\":1,\"b\":2},\"colorTemInKelvin\":3}}}";
    private final String lightBrightnessJsonString = "{\"msg\":{\"cmd\":\"brightness\",\"data\":{\"value\":99}}}";
    private static final String lightQueryJsonString = "{\"msg\":{\"cmd\":\"devStatus\",\"data\":{}}}";

    @Test
    public void testSerializeMessage() {
        GenericGoveeMessage lightOff = new GenericGoveeMessage(new GenericGoveeMsg("turn", new ValueData(0)));
        assertEquals(lightOffJsonString, GSON.toJson(lightOff));
        GenericGoveeMessage lightOn = new GenericGoveeMessage(new GenericGoveeMsg("brightness", new ValueData(100)));
        assertEquals(lightOnJsonString, GSON.toJson(lightOn));
        GenericGoveeMessage lightColor = new GenericGoveeMessage(
                new GenericGoveeMsg("colorwc", new ColorData(new Color(0, 1, 2), 3)));
        assertEquals(lightColorJsonString, GSON.toJson(lightColor));
        GenericGoveeMessage lightBrightness = new GenericGoveeMessage(
                new GenericGoveeMsg("brightness", new ValueData(99)));
        assertEquals(lightBrightnessJsonString, GSON.toJson(lightBrightness));
        GenericGoveeMessage lightQuery = new GenericGoveeMessage(
                new GenericGoveeMsg("devStatus", new EmptyValueQueryStatusData()));
        assertEquals(lightQueryJsonString, GSON.toJson(lightQuery));
    }
}
