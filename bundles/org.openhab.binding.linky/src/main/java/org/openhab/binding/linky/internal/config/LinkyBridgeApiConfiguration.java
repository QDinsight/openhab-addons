/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.linky.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LinkyBridgeApiConfiguration} is the class used to match the
 * thing configuration.
 *
 * @author Gaël L'hopital - Initial contribution
 * @author Laurent Arnal - Rewrite addon to use official dataconect API
 */
@NonNullByDefault
public class LinkyBridgeApiConfiguration extends LinkyBridgeConfiguration {
    public String clientId = "";
    public String clientSecret = "";
    public boolean isSandbox = false;

    public boolean seemsValid() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
