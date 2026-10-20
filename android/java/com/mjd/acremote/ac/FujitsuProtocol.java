package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Fujitsu (فوجيتسو / فوجي) - 16 byte / 128 bit protocol. */
public class FujitsuProtocol extends Protocol {
    private static final int HDR_MARK = 3324, HDR_SPACE = 1574;
    private static final int BIT_MARK = 448, ONE_SPACE = 1182, ZERO_SPACE = 390;
    private static final int GAP = 8100;

    private boolean lastPower = false;

    public FujitsuProtocol() {
        super("fujitsu", "فوجيتسو", "Fujitsu", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        if (!s.power) {
            // Fujitsu uses a special short 7 byte "off" message.
            int[] off = {0x14, 0x63, 0x00, 0x10, 0x10, 0x02, 0xFD};
            debugRaw = off.clone();
            byte[] data = new byte[7];
            for (int i = 0; i < 7; i++) data[i] = (byte) off[i];
            PulseBuilder p = new PulseBuilder();
            p.mark(HDR_MARK).space(HDR_SPACE);
            p.bytesLSB(data, 0, 7, BIT_MARK, ONE_SPACE, ZERO_SPACE);
            p.mark(BIT_MARK).space(GAP);
            return p.build(38000);
        }
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
            case AcState.FAN_LOW: fan = 3; break;
            case AcState.FAN_MED: fan = 2; break;
            case AcState.FAN_HIGH: fan = 1; break;
            default: fan = 0; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);
        int swing = s.swingV && s.swingH ? 3 : (s.swingV ? 1 : (s.swingH ? 2 : 0));

        int[] b = new int[16];
        b[0] = 0x14;
        b[1] = 0x63;
        b[3] = 0x10;
        b[4] = 0x10;
        b[5] = 0xFE;                                  // "stay on" command for ARRAH2E
        b[6] = 9;                                     // bytes remaining
        b[7] = 0x30;                                  // protocol version
        b[8] = ((temp - 16) * 16) & 0xFC;             // (temp - 16) * 4, shifted by 2
        b[9] = mode & 0x07;
        b[10] = (fan & 0x07) | ((swing & 0x03) << 4);
        b[14] = 0x20;
        int sum = 0;
        for (int i = 7; i <= 14; i++) sum += b[i];
        b[15] = (0 - sum) & 0xFF;

        debugRaw = b.clone();
        byte[] data = new byte[16];
        for (int i = 0; i < 16; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        // Two identical frames: the first one is often missed by the A/C.
        for (int rep = 0; rep < 2; rep++) {
            p.mark(HDR_MARK).space(HDR_SPACE);
            p.bytesLSB(data, 0, 16, BIT_MARK, ONE_SPACE, ZERO_SPACE);
            p.mark(BIT_MARK).space(GAP);
        }
        return p.build(38000);
    }
}