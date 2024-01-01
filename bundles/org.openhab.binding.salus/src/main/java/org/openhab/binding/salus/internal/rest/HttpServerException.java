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
package org.openhab.binding.salus.internal.rest;

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Martin Grześlowski - Initial contribution
 */
@SuppressWarnings("SerializableHasSerializationMethods")
@NonNullByDefault
public class HttpServerException extends HttpException {
    @Serial
    private static final long serialVersionUID = 1L;

    public HttpServerException(int code, String method, String url) {
        super(code, "Server Error", method, url);
    }
}
