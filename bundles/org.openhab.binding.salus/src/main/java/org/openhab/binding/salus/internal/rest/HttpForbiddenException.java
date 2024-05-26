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
package org.openhab.binding.salus.internal.rest;

import java.io.Serial;

/**
 * @author Martin Grześlowski - Initial contribution
 */
public class HttpForbiddenException extends HttpException {
    @Serial
    private static final long serialVersionUID = 1L;

    public HttpForbiddenException(String method, String url) {
        super(403, "Forbidden", method, url);
    }
}
