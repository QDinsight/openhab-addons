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
package org.openhab.binding.bluetooth.bluegiga.internal.enumeration;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Class to implement the BlueGiga Enumeration <b>SmpIoCapabilities</b>.
 * <p>
 * Security Manager I/O Capabilities
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public enum SmpIoCapabilities {
    /**
     * Default unknown value
     */
    UNKNOWN(-1),

    /**
     * [0] Display Only
     */
    SM_IO_CAPABILITY_DISPLAYONLY(0x0000),

    /**
     * [1] Display with Yes/No-buttons
     */
    SM_IO_CAPABILITY_DISPLAYYESNO(0x0001),

    /**
     * [2] Keyboard Only
     */
    SM_IO_CAPABILITY_KEYBOARDONLY(0x0002),

    /**
     * [3] No Input and No Output
     */
    SM_IO_CAPABILITY_NOINPUTNOOUTPUT(0x0003),

    /**
     * [4] Display with Keyboard
     */
    SM_IO_CAPABILITY_KEYBOARDDISPLAY(0x0004);

    /**
     * A mapping between the integer code and its corresponding type to
     * facilitate lookup by code.
     */
    private static @Nullable Map<Integer, SmpIoCapabilities> codeMapping;

    private int key;

    private SmpIoCapabilities(int key) {
        this.key = key;
    }

    /**
     * Lookup function based on the type code. Returns {@link UNKNOWN} if the code does not exist.
     *
     * @param smpIoCapabilities
     *            the code to lookup
     * @return enumeration value.
     */
    public static SmpIoCapabilities getSmpIoCapabilities(int smpIoCapabilities) {
        Map<Integer, SmpIoCapabilities> localCodeMapping = codeMapping;
        if (localCodeMapping == null) {
            localCodeMapping = new HashMap<>();
            for (SmpIoCapabilities s : values()) {
                localCodeMapping.put(s.key, s);
            }
            codeMapping = localCodeMapping;
        }

        return localCodeMapping.getOrDefault(smpIoCapabilities, UNKNOWN);
    }

    /**
     * Returns the BlueGiga protocol defined value for this enum
     *
     * @return the BGAPI enumeration key
     */
    public int getKey() {
        return key;
    }
}
