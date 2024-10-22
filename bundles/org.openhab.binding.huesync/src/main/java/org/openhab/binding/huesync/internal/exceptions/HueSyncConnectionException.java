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
package org.openhab.binding.huesync.internal.exceptions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;

/**
 *
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
public class HueSyncConnectionException extends HueSyncException {
    private static final long serialVersionUID = 0L;

    public HueSyncConnectionException(String message, Logger logger) {
        super(message, logger);
    }
}
