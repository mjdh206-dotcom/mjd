package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Hitachi AC1 (هيتاشي) - 13 byte / 104 bit protocol. */
public class HitachiProtocol extends Protocol {
    private static final int HDR_MARK = 3400, HDR_SPACE = 3400;
    private static final int BIT_MARK = 400, ONE_SPACE = 1250, ZERO_SPACE = 500;
    private static final int GAP = 100000;

    private static final int[] RESET = {
            0xB2, 0xAE, 0x4D, 0x91, 0xF0, 0xE1, 0xA4,
            0x00, 0x00, 0x00, 0x00, 0x61, 0x24};

    private boolean powerToggle = true;

    public HitachiProtocol() {
        super("hitachi", "هيتاشي", "Hitachi", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = RESET.clone();
        int mode;
        switch (s.mode) {
            case AcState.MODE_DRY: mode = 0b0010; break;
            case AcState.MODE_FAN: mode = 0b0100; break;
            case AcState.MODE_HEAT: mode = 0b1001; break;
            case AcState.MODE_AUTO: mode = 0b1110; break;
            default: mode = 0b0110; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 0b1000; break;
            case AcState.FAN_MED: fan = 0b0100; break;
            case AcState.FAN_HIGH: fan = 0b0010; break;
            default: fan = 0b0001; break;
        }
        if (mode == 0b1110) fan = 0b0001;          // auto mode locks fan to auto
        if (mode == 0b0010) fan = 0b1000;          // dry locks fan to low
        int temp = clamp(s.temp, minTemp, maxTemp);

        b[3] |= 0b10 << 6;                          // model A
        b[5] = (fan & 0x0F) | ((mode & 0x0F) << 4);
        b[6] = (reverse(temp - 7, 5) & 0x1F) << 2;
        b[11] = (s.power ? 0x20 : 0x00)
                | (powerToggle ? 0x10 : 0x00)
                | (s.swingV ? 0x40 : 0x00)
                | (s.swingH ? 0x80 : 0x00);
        powerToggle = !powerToggle;
        b[12] = checksum(b);

        byte[] data = new byte[13];
        for (int i = 0; i < 13; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesMSB(data, 0, 13, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        return p.build(38000);
    }

    private static int checksum(int[] b) {
        int sum = 0;
        for (int i = 5; i < 12; i++) {
            sum += reverse(b[i] & 0x0F, 4);
            sum += reverse((b[i] >> 4) & 0x0F, 4);
        }
        return reverse(sum & 0xFF, 8) & 0xFF;
    }

    private static int reverse(int value, int bits) {
        int out = 0;
        for (int i = 0; i < bits; i++) {
            out = (out << 1) | ((value >> i) & 1);
        }
        return out;
    }
}
