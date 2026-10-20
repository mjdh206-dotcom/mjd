package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Midea (ميديا) - 48 bit protocol, sent as data + inverted data. */
public class MideaProtocol extends Protocol {
    private static final int TICK = 80;
    private static final int HDR_MARK = 56 * TICK;   // 4480
    private static final int HDR_SPACE = 56 * TICK;
    private static final int BIT_MARK = 7 * TICK;    // 560
    private static final int ONE_SPACE = 21 * TICK;  // 1680
    private static final int ZERO_SPACE = 7 * TICK;  // 560
    private static final int MIN_GAP = 70 * TICK;    // 5600

    public MideaProtocol() {
        super("midea", "ميديا", "Midea", 17, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int mode;
        switch (s.mode) {
            case AcState.MODE_DRY: mode = 1; break;
            case AcState.MODE_AUTO: mode = 2; break;
            case AcState.MODE_HEAT: mode = 3; break;
            case AcState.MODE_FAN: mode = 4; break;
            default: mode = 0; break;
        }
        int fan = Math.min(3, Math.max(0, s.fan));
        int temp = clamp(s.temp, minTemp, maxTemp);

        int[] b = new int[6];
        b[1] = 0x7F;                                   // sensor disabled
        b[2] = 0x01;                                   // no timers
        b[3] = (temp - 17) & 0x1F;
        b[4] = (mode & 0x07) | ((fan & 0x03) << 3)
                | (s.sleep ? 0x40 : 0) | (s.power ? 0x80 : 0);
        b[5] = 0xA1;                                   // type 1 + header 0b10100

        int sum = 0;
        for (int i = 1; i <= 5; i++) sum += Integer.reverse(b[i] & 0xFF) >>> 24;
        sum = (256 - (sum & 0xFF)) & 0xFF;
        b[0] = Integer.reverse(sum) >>> 24;

        byte[] data = new byte[6];
        for (int i = 0; i < 6; i++) data[i] = (byte) b[i];
        byte[] inv = new byte[6];
        for (int i = 0; i < 6; i++) inv[i] = (byte) ~data[i];

        PulseBuilder p = new PulseBuilder();
        for (int phase = 0; phase < 2; phase++) {
            byte[] d = phase == 0 ? data : inv;
            p.mark(HDR_MARK).space(HDR_SPACE);
            for (int i = 0; i < 6; i++) {
                // Each byte MSB-first followed by its inverse.
                p.bitsMSB(d[i] & 0xFF, 8, BIT_MARK, ONE_SPACE, ZERO_SPACE);
                p.bitsMSB((~d[i]) & 0xFF, 8, BIT_MARK, ONE_SPACE, ZERO_SPACE);
            }
            p.mark(BIT_MARK).space(MIN_GAP);
        }
        return p.build(38000);
    }
}
