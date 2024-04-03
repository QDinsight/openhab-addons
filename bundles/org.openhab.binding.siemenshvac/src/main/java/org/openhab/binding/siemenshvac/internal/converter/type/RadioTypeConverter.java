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
package org.openhab.binding.siemenshvac.internal.converter.type;

import org.openhab.binding.siemenshvac.internal.converter.ConverterException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Converts between a SiemensHvac datapoint value and an openHAB DecimalType.
 *
 * @author Laurent Arnal - Initial contribution
 */
public class RadioTypeConverter extends AbstractTypeConverter<DecimalType> {
    @Override
    protected boolean toBindingValidation(JsonObject dp, Class<? extends Type> typeClass) {
        return true;
    }

    @Override
    protected Object toBinding(DecimalType type, JsonObject dp) throws ConverterException {
        return null;
    }

    @Override
    protected boolean fromBindingValidation(JsonElement value, String type) {
        return true;
    }

    @Override
    protected DecimalType fromBinding(JsonElement value, String type) throws ConverterException {
        DecimalType updateVal = new DecimalType();
        String valueSt = value.getAsString();

        if ("Arrêt".equals(valueSt)) {
            updateVal = new DecimalType(0);
        } else if ("Marche".equals(valueSt)) {
            updateVal = new DecimalType(1);
        }

        return updateVal;
    }
}
