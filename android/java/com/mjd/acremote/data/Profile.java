package com.mjd.acremote.data;

import com.mjd.acremote.ac.AcState;

/** A saved air-conditioner / remote profile. */
public class Profile {
    public String id;
    public String name;
    public String protocolId;
    public int variant = 0;
    public int temp = 24;
    public int mode = AcState.MODE_COOL;
    public int fan = AcState.FAN_AUTO;
    public boolean power = false;
    public boolean swingV = false;
    public boolean swingH = false;
    public boolean turbo = false;
    public boolean sleep = false;
    public boolean light = true;
    public boolean econo = false;

    public Profile() {
    }

    public Profile(String id, String name, String protocolId) {
        this.id = id;
        this.name = name;
        this.protocolId = protocolId;
    }

    public AcState toState() {
        AcState s = new AcState();
        s.power = power;
        s.temp = temp;
        s.mode = mode;
        s.fan = fan;
        s.swingV = swingV;
        s.swingH = swingH;
        s.turbo = turbo;
        s.sleep = sleep;
        s.light = light;
        s.econo = econo;
        return s;
    }

    public void fromState(AcState s) {
        temp = s.temp;
        mode = s.mode;
        fan = s.fan;
        power = s.power;
        swingV = s.swingV;
        swingH = s.swingH;
        turbo = s.turbo;
        sleep = s.sleep;
        light = s.light;
        econo = s.econo;
    }
}
