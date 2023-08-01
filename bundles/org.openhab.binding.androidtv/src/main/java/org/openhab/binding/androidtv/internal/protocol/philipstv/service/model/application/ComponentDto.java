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
package org.openhab.binding.androidtv.internal.protocol.philipstv.service.model.application;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of {@link LaunchAppDto} and {@link CurrentAppDto}
 *
 * @author Benjamin Meyer - Initial contribution
 */
public class ComponentDto {

    @JsonProperty
    private String className;

    @JsonProperty
    private String packageName;

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toString() {
        return "Component{" + "className = '" + className + '\'' + ",packageName = '" + packageName + '\'' + "}";
    }
}
