package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Daikin (دايكين) - 35 byte / 280 bit protocol, sent in three sections. */
public class DaikinProtocol extends Protocol {
    private static final int HDR_MARK = 3650, HDR_SPACE = 1623;
    private static final int BIT_MARK = 428, ONE_SPACE = 1280, ZERO_SPACE = 428;
    private static final int GAP = 29000;

    public DaikinProtocol() {
        super("daikin", "دايكين", "Daikin", 10, 32, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = new int[35];
        b[0] = 0x11; b[1] = 0xDA; b[2] = 0x27; b[4] = 0xC5;
        b[8] = 0x11; b[9] = 0xDA; b[10] = 0x27; b[12] = 0x42;
        b[16] = 0x11; b[17] = 0xDA; b[18] = 0x27;
        b[21] = 0x49; b[22] = 0x1E; b[24] = 0xB0;
        b[27] = 0x06; b[28] = 0x60; b[31] = 0xC0;

        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0b000; break;
            case AcState.MODE_DRY: mode = 0b010; break;
            case AcState.MODE_COOL: mode = 0b011; break;
            case AcState.MODE_HEAT: mode = 0b100; break;
            default: mode = 0b110; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 3; break;
            case AcState.FAN_MED: fan = 5; break;
            case AcState.FAN_HIGH: fan = 7; break;
            default: fan = 0x0A; break;
        }
        b[21] = (s.power ? 0x01 : 0x00) | 0x08 | ((mode & 0x07) << 4);
        b[22] = (temp * 2) & 0xFF;
        b[24] = ((fan & 0x0F) << 4) | (s.swingV ? 0x0F : 0x00);
        b[25] = (s.swingH ? 0x0F : 0x00);

        b[7] = sum(b, 0, 7);
        b[15] = sum(b, 8, 7);
        b[34] = sum(b, 16, 18);

        debugRaw = b.clone();
        byte[] data = new byte[35];
        for (int i = 0; i < 35; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        // 5-bit "00000" header
        p.bitsLSB(0, 5, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(ZERO_SPACE + GAP);
        int[] offs = {0, 8, 16};
        int[] lens = {8, 8, 19};
        for (int sec = 0; sec < 3; sec++) {
            p.mark(HDR_MARK).space(HDR_SPACE);
            p.bytesLSB(data, offs[sec], lens[sec], BIT_MARK, ONE_SPACE, ZERO_SPACE);
            p.mark(BIT_MARK).space(ZERO_SPACE + GAP);
        }
        return p.build(38000);
    }

    private static int sum(int[] b, int off, int len) {
        int s = 0;
        for (int i = 0; i < len; i++) s += b[off + i];
        return s & 0xFF;
    }
}
