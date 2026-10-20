package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Coolix / Midea-family 24-bit protocol used by many OEM brands. */
public class CoolixProtocol extends Protocol {
    private static final int TICK = 276;
    private static final int HDR_MARK = 17 * TICK;   // 4692
    private static final int HDR_SPACE = 16 * TICK;  // 4416
    private static final int BIT_MARK = 2 * TICK;    // 552
    private static final int ONE_SPACE = 6 * TICK;   // 1656
    private static final int ZERO_SPACE = 2 * TICK;  // 552
    private static final int MIN_GAP = 19 * TICK;    // 5244

    private static final int[] TEMP_MAP = {
            0b0000, 0b0001, 0b0011, 0b0010, 0b0110, 0b0111, 0b0101,
            0b0100, 0b1100, 0b1101, 0b1001, 0b1000, 0b1010, 0b1011};

    private static final int CMD_OFF = 0xB27BE0;
    private static final int SENSOR_IGNORE = 0x1F;

    public CoolixProtocol() {
        super("coolix", "كوليكس (ميديا/أو إي إم)", "Coolix", 17, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int raw;
        if (!s.power) {
            raw = CMD_OFF;
        } else {
            int mode;
            switch (s.mode) {
                case AcState.MODE_DRY: mode = 1; break;
                case AcState.MODE_AUTO: mode = 2; break;
                case AcState.MODE_HEAT: mode = 3; break;
                case AcState.MODE_FAN: mode = 1; break; // fan = dry + special temp code
                default: mode = 0; break;
            }
            int fan;
            switch (s.fan) {
                case AcState.FAN_LOW: fan = 4; break;   // kCoolixFanMin
                case AcState.FAN_MED: fan = 2; break;   // kCoolixFanMed
                case AcState.FAN_HIGH: fan = 1; break;  // kCoolixFanMax
                default: fan = (mode == 0 || mode == 2) ? 0 : 5;
            }
            if (mode == 0 || mode == 2) fan = 0;        // cool/heat use auto0 only in auto/dry
            if (s.mode == AcState.MODE_COOL || s.mode == AcState.MODE_HEAT) {
                if (s.fan == AcState.FAN_AUTO) fan = 5;
            }
            int tempCode;
            if (s.mode == AcState.MODE_FAN) {
                tempCode = 0b1110;
            } else {
                int t = clamp(s.temp, minTemp, maxTemp);
                tempCode = TEMP_MAP[t - 17];
            }
            raw = 0xB00000 | (fan << 13) | (SENSOR_IGNORE << 8) | (tempCode << 4) | (mode << 2);
        }
        return build(raw);
    }

    private Signal build(int raw) {
        PulseBuilder p = new PulseBuilder();
        for (int rep = 0; rep < 2; rep++) {
            p.mark(HDR_MARK).space(HDR_SPACE);
            for (int i = 2; i >= 0; i--) {
                int segment = (raw >> (i * 8)) & 0xFF;
                p.bitsMSB(segment, 8, BIT_MARK, ONE_SPACE, ZERO_SPACE);
                p.bitsMSB(segment ^ 0xFF, 8, BIT_MARK, ONE_SPACE, ZERO_SPACE);
            }
            p.mark(BIT_MARK).space(MIN_GAP);
        }
        return p.build(38000);
    }
}
