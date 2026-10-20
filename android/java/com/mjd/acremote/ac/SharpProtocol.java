package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Sharp (شارب) - 13 byte protocol. */
public class SharpProtocol extends Protocol {
    private static final int HDR_MARK = 3800, HDR_SPACE = 1900;
    private static final int BIT_MARK = 470, ONE_SPACE = 1400, ZERO_SPACE = 500;
    private static final int GAP = 100000;

    private static final int[] RESET = {
            0xAA, 0x5A, 0xCF, 0x10, 0x00, 0x01, 0x00,
            0x00, 0x08, 0x80, 0x00, 0xE0, 0x01};

    private boolean lastPower = false;

    public SharpProtocol() {
        super("sharp", "شارب", "Sharp", 15, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = RESET.clone();
        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0; break;
            case AcState.MODE_HEAT: mode = 1; break;
            case AcState.MODE_COOL: mode = 2; break;
            case AcState.MODE_DRY: mode = 3; break;
            default: mode = 2; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 4; break;
            case AcState.FAN_MED: fan = 3; break;
            case AcState.FAN_HIGH: fan = 5; break;
            default: fan = 2; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);

        if (mode == 0 || mode == 3) {
            b[4] = 0x00;                                   // auto/dry: fixed temp
            fan = 2;                                       // fan forced to auto
        } else {
            b[4] = 0xC0 | ((temp - 15) & 0x0F);
        }
        b[5] = s.power ? 0x11 : 0x21;                      // on-from-off / off
        b[6] = (mode & 0x03) | ((fan & 0x07) << 4);
        b[8] = 0x08 | (s.swingV ? 0x07 : 0x02);            // swing toggle / fixed
        b[10] = 0x00;                                      // "power" button marker

        int x = 0;
        for (int i = 0; i < 12; i++) x ^= b[i];
        x ^= b[12] & 0x0F;
        x ^= (x >> 4) & 0x0F;
        b[12] = ((x & 0x0F) << 4) | (b[12] & 0x0F);

        debugRaw = b.clone();
        byte[] data = new byte[13];
        for (int i = 0; i < 13; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesLSB(data, 0, 13, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        return p.build(38000);
    }
}