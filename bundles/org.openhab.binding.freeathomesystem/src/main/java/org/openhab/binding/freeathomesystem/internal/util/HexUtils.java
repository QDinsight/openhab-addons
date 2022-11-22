/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.freeathomesystem.internal.util;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * helper class to work with hex numbers
 *
 * @author Andras Uhrin - Initial contribution
 *
 */
@NonNullByDefault
public class HexUtils {

    public static int getIntegerFromHex(String strHexValue) {
        String digits = "0123456789ABCDEF";
        String str = strHexValue.toUpperCase();
        int hexval = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int d = digits.indexOf(c);
            hexval = 16 * hexval + d;
        }

        return hexval;
    }
}
