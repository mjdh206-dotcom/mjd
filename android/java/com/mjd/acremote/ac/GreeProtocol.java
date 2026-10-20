package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Gree (جري) - 8 byte / 64 bit protocol. */
public class GreeProtocol extends Protocol {
    // Timings (microseconds)
    private static final int HDR_MARK = 9000, HDR_SPACE = 4500;
    private static final int BIT_MARK = 620, ONE_SPACE = 1600, ZERO_SPACE = 540;
    private static final int MSG_SPACE = 19980;

    public GreeProtocol() {
        super("gree", "جري", "Gree", 16, 30, true, true, true, true);
    }

    private boolean forceModelA = true;

    @Override
    public int variantCount() {
        return 2;
    }

    @Override
    public Signal testSignal(int variant) {
        forceModelA = variant == 0;
        Signal sig = encode(defaultState());
        forceModelA = true;
        return sig;
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = new int[8];
        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0; break;
            case AcState.MODE_DRY: mode = 2; break;
            case AcState.MODE_FAN: mode = 3; break;
            case AcState.MODE_HEAT: mode = 4; break;
            default: mode = 1; break;
        }
        if (s.econo) mode = 5;
        int fan = Math.min(3, Math.max(0, s.fan));
        int temp = clamp(s.temp, minTemp, maxTemp);

        b[0] = (mode & 0x07)
                | (s.power ? 0x08 : 0)
                | ((fan & 0x03) << 4)
                | (s.swingV ? 0x40 : 0)
                | (s.sleep ? 0x80 : 0);
        b[1] = (temp - 16) & 0x0F;                 // no timers
        b[2] = 0x20;                                // display light on
        if (s.turbo) b[2] |= 0x10;
        if (s.power && forceModelA) b[2] |= 0x40;   // YAW1F model bit
        b[3] = 0x50;                                // constant
        int swingV = s.swingV ? 1 : 0;              // 1 = auto swing
        int swingH = s.swingH ? 1 : 0;
        b[4] = (swingV & 0x0F) | ((swingH & 0x07) << 4);
        b[5] = 0x20;                                // constant
        b[6] = 0x00;

        int sum = 10;
        for (int i = 0; i < 4; i++) sum += b[i] & 0x0F;
        for (int i = 4; i < 7; i++) sum += (b[i] >> 4) & 0x0F;
        b[7] = (sum & 0x0F) << 4;

        byte[] data = new byte[8];
        for (int i = 0; i < 8; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        // Block #1 (4 bytes) + footer 0b010
        p.bytesLSB(data, 0, 4, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.bitsLSB(0b010, 3, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(MSG_SPACE);
        // Block #2 (4 bytes)
        p.bytesLSB(data, 4, 4, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(MSG_SPACE);
        return p.build(38000);
    }
}
