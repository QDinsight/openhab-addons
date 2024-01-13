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
package org.openhab.binding.bluetooth.bluegiga.internal.command.connection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaDeviceResponse;

/**
 * Class to implement the BlueGiga command <b>featureIndEvent</b>.
 * <p>
 * This event indicates the remote devices features.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaFeatureIndEvent extends BlueGigaDeviceResponse {
    public static final int COMMAND_CLASS = 0x03;
    public static final int COMMAND_METHOD = 0x02;

    /**
     * CtrData field from LL_FEATURE_RSP - packet
     * <p>
     * BlueGiga API type is <i>uint8array</i> - Java type is {@link int[]}
     */
    private int[] features;

    /**
     * Event constructor
     */
    public BlueGigaFeatureIndEvent(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        event = (inputBuffer[0] & 0x80) != 0;

        // Deserialize the fields
        connection = deserializeUInt8();
        features = deserializeUInt8Array();
    }

    /**
     * CtrData field from LL_FEATURE_RSP - packet
     * <p>
     * BlueGiga API type is <i>uint8array</i> - Java type is {@link int[]}
     *
     * @return the current features as {@link int[]}
     */
    public int[] getFeatures() {
        return features;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaFeatureIndEvent [connection=");
        builder.append(connection);
        builder.append(", features=");
        for (int c = 0; c < features.length; c++) {
            if (c > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", features[c]));
        }
        builder.append(']');
        return builder.toString();
    }
}
