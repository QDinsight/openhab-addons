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
package org.openhab.binding.growatt.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.growatt.internal.GrowattBindingConstants;
import org.openhab.binding.growatt.internal.GrowattBindingConstants.UoM;
import org.openhab.binding.growatt.internal.dto.GrottDevice;
import org.openhab.binding.growatt.internal.dto.GrottValues;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;

import com.google.gson.Gson;

/**
 * The {@link GrowattTest} is a JUnit test suite for the Growatt binding.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class GrowattTest {

    private final Gson gson = new Gson();

    /**
     * load a string from a file
     */
    private String load(String fileName) {
        try (FileReader file = new FileReader(String.format("src/test/resources/%s.json", fileName));
                BufferedReader reader = new BufferedReader(file)) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return builder.toString();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return "";
    }

    /**
     * Load a GrottValues class from a JSON payload.
     *
     * @param fileName the file containing the json payload.
     * @return a GrottValues dto.
     */
    private GrottValues loadGrottValues(String fileName) {
        String json = load(fileName);
        GrottDevice device = gson.fromJson(json, GrottDevice.class);
        assertNotNull(device);
        GrottValues grottValues = device.getValues();
        assertNotNull(grottValues);
        return grottValues;
    }

    /**
     * Test that GrottValues implements the same fields as thye GrowattBindingConstants.CHANNEL_ID_UOM_MAP.
     * Test that all fields can be accessed and that they are either null or an Integer instance.
     */
    @Test
    void testGrottValuesAccessibility() {
        GrottValues grottValues = loadGrottValues("simple");

        List<String> fields = Arrays.asList(GrottValues.class.getFields()).stream().map(f -> f.getName())
                .collect(Collectors.toList());

        for (String channel : GrowattBindingConstants.CHANNEL_ID_UOM_MAP.keySet()) {
            assertTrue(fields.contains(GrottValues.getFieldName(channel)));
        }

        for (String field : fields) {
            assertTrue(GrowattBindingConstants.CHANNEL_ID_UOM_MAP.containsKey(GrottValues.getChannelId(field)));
        }

        assertEquals(fields.size(), GrowattBindingConstants.CHANNEL_ID_UOM_MAP.size());

        for (Entry<String, UoM> entry : GrowattBindingConstants.CHANNEL_ID_UOM_MAP.entrySet()) {
            String channelId = entry.getKey();
            Field field;
            try {
                field = GrottValues.class.getField(GrottValues.getFieldName(channelId));
            } catch (NoSuchFieldException e) {
                fail(e.getMessage());
                continue;
            } catch (SecurityException e) {
                fail(e.getMessage());
                continue;
            }
            try {
                Object value = field.get(grottValues);
                assertTrue(value == null || (value instanceof Integer));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                fail(e.getMessage());
                continue;
            }
        }
    }

    /**
     * Test that GrottValues is loaded with the correct contents from a JSON file.
     */
    @Test
    void testGrottValuesContents() {
        GrottValues grottValues = loadGrottValues("simple");
        Map<String, QuantityType<?>> channelStates = null;

        try {
            channelStates = grottValues.getChannelStates();
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        } catch (SecurityException e) {
            fail(e.getMessage());
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }

        assertNotNull(channelStates);
        assertEquals(29, channelStates.size());

        channelStates.forEach((channelId, state) -> {
            assertTrue(state instanceof QuantityType<?>);
        });

        assertEquals(QuantityType.ONE, channelStates.get("status"));
        assertEquals(QuantityType.valueOf(235.3, Units.VOLT), channelStates.get("grid-potential"));
        assertEquals(QuantityType.valueOf(0.7, Units.AMPERE), channelStates.get("grid-current"));
        assertEquals(QuantityType.valueOf(146, Units.WATT), channelStates.get("grid-power"));
        assertEquals(QuantityType.valueOf(49.97, Units.HERTZ), channelStates.get("grid-frequency"));
        assertEquals(QuantityType.valueOf(27.3, SIUnits.CELSIUS), channelStates.get("pv-temperature"));
        assertEquals(QuantityType.valueOf(4545.3, Units.KILOWATT_HOUR), channelStates.get("pv-grid-energy-total"));

        State state = channelStates.get("total-work-time");
        assertTrue(state instanceof QuantityType<?>);
        if (state instanceof QuantityType<?> quantity) {
            QuantityType<?> seconds = quantity.toUnit(Units.SECOND);
            assertNotNull(seconds);
            assertEquals(QuantityType.valueOf(32751939, Units.SECOND).doubleValue(), seconds.doubleValue(), 0.1);
        }
    }
}
