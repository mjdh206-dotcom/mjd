package com.mjd.acremote.ac;

/** A generic air-conditioner state shared by all protocols. */
public class AcState {
    public static final int MODE_AUTO = 0;
    public static final int MODE_COOL = 1;
    public static final int MODE_DRY = 2;
    public static final int MODE_FAN = 3;
    public static final int MODE_HEAT = 4;

    public static final int FAN_AUTO = 0;
    public static final int FAN_LOW = 1;
    public static final int FAN_MED = 2;
    public static final int FAN_HIGH = 3;

    public boolean power = true;
    public int mode = MODE_COOL;
    public int temp = 24;
    public int fan = FAN_AUTO;
    public boolean swingV = false;
    public boolean swingH = false;
    public boolean turbo = false;
    public boolean sleep = false;
    public boolean light = true;
    public boolean econo = false;

    public AcState copy() {
        AcState s = new AcState();
        s.power = power;
        s.mode = mode;
        s.temp = temp;
        s.fan = fan;
        s.swingV = swingV;
        s.swingH = swingH;
        s.turbo = turbo;
        s.sleep = sleep;
        s.light = light;
        s.econo = econo;
        return s;
    }

    public static String modeNameAr(int mode) {
        switch (mode) {
            case MODE_AUTO: return "تلقائي";
            case MODE_DRY: return "تجفيف";
            case MODE_FAN: return "مروحة";
            case MODE_HEAT: return "تدفئة";
            default: return "تبريد";
        }
    }

    public static String fanNameAr(int fan) {
        switch (fan) {
            case FAN_LOW: return "منخفضة";
            case FAN_MED: return "متوسطة";
            case FAN_HIGH: return "عالية";
            default: return "تلقائي";
        }
    }

    public static String modeIcon(int mode) {
        switch (mode) {
            case MODE_AUTO: return "❄";
            case MODE_DRY: return "💧";
            case MODE_FAN: return "🌀";
            case MODE_HEAT: return "☀";
            default: return "❄";
        }
    }
}
