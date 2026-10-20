package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Kelvinator (كلفيناتور) - 16 byte protocol in two blocks. */
public class KelvinatorProtocol extends Protocol {
    private static final int TICK = 85;
    private static final int HDR_MARK = 106 * TICK;   // 9010
    private static final int HDR_SPACE = 53 * TICK;   // 4505
    private static final int BIT_MARK = 8 * TICK;     // 680
    private static final int ONE_SPACE = 18 * TICK;   // 1530
    private static final int ZERO_SPACE = 6 * TICK;   // 510
    private static final int GAP = 235 * TICK;        // 19975

    public KelvinatorProtocol() {
        super("kelvinator", "كلفيناتور", "Kelvinator", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = new int[16];
        b[3] = 0x50;
        b[11] = 0x70;

        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0; break;
            case AcState.MODE_COOL: mode = 1; break;
            case AcState.MODE_DRY: mode = 2; break;
            case AcState.MODE_FAN: mode = 3; break;
            default: mode = 4; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 2; break;
            case AcState.FAN_MED: fan = 3; break;
            case AcState.FAN_HIGH: fan = 5; break;
            default: fan = 0; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);
        if (mode == 0 || mode == 2) temp = 25;

        b[0] = (mode & 0x07)
                | (s.power ? 0x08 : 0)
                | ((Math.min(3, fan) & 0x03) << 4)
                | (s.swingV ? 0x40 : 0);
        b[1] = (temp - 16) & 0x0F;
        b[2] = (s.turbo ? 0x10 : 0) | (s.light ? 0x20 : 0)
                | (s.econo ? 0x80 : 0);                       // ion filter bit reuse
        b[4] = (s.swingV ? 0x01 : 0x00) | (s.swingH ? 0x10 : 0x00);
        b[8] = b[0];
        b[9] = b[1];
        b[10] = b[2];
        b[7] = blockChecksum(b, 0) << 4;
        b[15] = blockChecksum(b, 8) << 4;
        b[14] = (fan & 0x07) << 4;

        debugRaw = b.clone();
        byte[] data = new byte[16];
        for (int i = 0; i < 16; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesLSB(data, 0, 4, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.bitsLSB(0b010, 3, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        p.bytesLSB(data, 4, 4, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP * 2);
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesLSB(data, 8, 4, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.bitsLSB(0b010, 3, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        p.bytesLSB(data, 12, 4, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP * 2);
        return p.build(38000);
    }

    private static int blockChecksum(int[] b, int offset) {
        int sum = 10;
        for (int i = 0; i < 4; i++) sum += b[offset + i] & 0x0F;
        for (int i = 4; i < 7; i++) sum += (b[offset + i] >> 4) & 0x0F;
        return sum & 0x0F;
    }
}
