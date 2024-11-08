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
package org.openhab.binding.metofficedatahub.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementations of this interface, allow access to a HttpClient which can be used
 * for communication requests to LinkTap Gateways.
 *
 * @author David Goodyear - Initial contribution
 */
@NonNullByDefault
public interface IConnectionStatusListener {
    void processAuthenticationResult(final boolean authenticated);

    void processCommunicationFailure(final @Nullable Throwable e);

    void processConnected();
}
