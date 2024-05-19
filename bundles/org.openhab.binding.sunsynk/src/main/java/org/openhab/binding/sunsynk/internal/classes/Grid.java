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

package org.openhab.binding.sunsynk.internal.classes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link Grid} is the internal class for Inverter real time grid information
 * from the SunSynk Account.
 * 
 * 
 * @author Lee Charlton - Initial contribution
 */

@NonNullByDefault

public class Grid {

    private int code;
    private String msg = "";
    private boolean success;
    private Data data = new Data();
    private double power;
    private double voltage;
    private double current;

    class Data {
        private List<VIP> vip = new ArrayList<VIP>();
        private int pac;
        private int qac;
        private double fac;
        private double pf;
        private int status;
        private double etodayFrom;
        private double etodayTo;
        private double etotalFrom;
        private double etotalTo;
        private List<Integer> limiterPowerArr = new ArrayList<Integer>();
        private int limiterTotalPowerArr;

        String content() {
            return "[pac: " + pac + "fac: " + fac + " quac: " + qac + " pf: " + pf + " status: " + status + "etoday: {"
                    + etodayFrom + ", " + etodayTo + "} etotal: {" + etotalFrom + ", " + etotalTo
                    + "} limiterPowerArr: " + limiterPowerArr + " limiterTotalPowerArr:" + limiterTotalPowerArr + "]";
        }
    }

    class VIP {
        private double volt;
        private double current;
        private double power;
    }

    public void sumVIP() {
        double l_power = 0.0;
        double l_voltage = 0.0;
        double l_current = 0.0;
        for (VIP x : this.data.vip) {
            l_power = l_power + x.power;
            l_voltage = l_voltage + x.volt;
            l_current = l_current + x.current;
        }
        this.power = l_power;
        this.voltage = l_voltage;
        this.current = l_current;
    }

    public double getGridPower() {
        // sumVIP();
        return this.power;
    }

    public double getGridVoltage() {
        // sumVIP();
        return this.voltage;
    }

    public double getGridCurrent() {
        // sumVIP();
        return this.current;
    }

    public String toString() {
        return "Content [code=" + code + ", msg=" + msg + "sucess=" + success + ", data=" + data.content() + "]";
    }
}
