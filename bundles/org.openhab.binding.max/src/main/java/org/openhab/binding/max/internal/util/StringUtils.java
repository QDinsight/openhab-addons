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
package org.openhab.binding.max.internal.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link StringUtils} class defines some static string utility methods
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class StringUtils {

    /**
     * Input string is shortened to the maxwidth, the last 3 chars are replaced by ...
     * 
     * For example: (maxWidth 18) input="openHAB is the greatest ever", return="openHAB is the ..."
     * 
     * @param input input string
     * @param maxWidth maxmimum amount of characters to return (including ...)
     * @return Abbreviated String
     */
    public static @Nullable String abbreviate(@Nullable String input, int maxWidth) {
        if (input != null) {
            if (input.length() <= 4 || input.length() <= maxWidth) {
                return input;
            }
            return input.substring(0, maxWidth - 3) + "...";
        }
        return input;
    }
}
