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
package org.openhab.binding.bluetooth.bluegiga.internal.command.connection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaDeviceCommand;

/**
 * Class to implement the BlueGiga command <b>update</b>.
 * <p>
 * This command updates the connection parameters of a given connection. The parameters have
 * the same meaning and follow the same rules as for the GAP class command: Connect Direct. If
 * this command is issued at a master device, it will send parameter update request to the link
 * layer. Bluetooth On the other hand if this command is issued at a slave device, it will send
 * L2CAP connection parameter update request to the master, which may either accept or reject
 * it. It will take an amount of time corresponding to at least six times the current connection
 * interval before the new connection parameters will become active.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaUpdateCommand extends BlueGigaDeviceCommand {
    public static final int COMMAND_CLASS = 0x03;
    public static final int COMMAND_METHOD = 0x02;

    /**
     * Minimum connection interval (units of 1.25ms)
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int intervalMin;

    /**
     * Maximum connection interval (units of 1.25ms)
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int intervalMax;

    /**
     * Slave latency which defines how many connections intervals a slave may skip.
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int latency;

    /**
     * Supervision timeout (units of 10ms)
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int timeout;

    /**
     * Minimum connection interval (units of 1.25ms)
     *
     * @param intervalMin the intervalMin to set as {@link int}
     */
    public void setIntervalMin(int intervalMin) {
        this.intervalMin = intervalMin;
    }

    /**
     * Maximum connection interval (units of 1.25ms)
     *
     * @param intervalMax the intervalMax to set as {@link int}
     */
    public void setIntervalMax(int intervalMax) {
        this.intervalMax = intervalMax;
    }

    /**
     * Slave latency which defines how many connections intervals a slave may skip.
     *
     * @param latency the latency to set as {@link int}
     */
    public void setLatency(int latency) {
        this.latency = latency;
    }

    /**
     * Supervision timeout (units of 10ms)
     *
     * @param timeout the timeout to set as {@link int}
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeUInt8(connection);
        serializeUInt16(intervalMin);
        serializeUInt16(intervalMax);
        serializeUInt16(latency);
        serializeUInt16(timeout);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaUpdateCommand [connection=");
        builder.append(connection);
        builder.append(", intervalMin=");
        builder.append(intervalMin);
        builder.append(", intervalMax=");
        builder.append(intervalMax);
        builder.append(", latency=");
        builder.append(latency);
        builder.append(", timeout=");
        builder.append(timeout);
        builder.append(']');
        return builder.toString();
    }
}
