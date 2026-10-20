package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Haier HSU07-HEA03 (هاير) - 9 byte protocol. */
public class HaierProtocol extends Protocol {
    private static final int HDR = 3000, HDR_GAP = 4300;
    private static final int BIT_MARK = 520, ONE_SPACE = 1650, ZERO_SPACE = 650;
    private static final int MIN_GAP = 150000;

    public HaierProtocol() {
        super("haier", "هاير", "Haier", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = new int[9];
        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0; break;
            case AcState.MODE_COOL: mode = 1; break;
            case AcState.MODE_DRY: mode = 2; break;
            case AcState.MODE_HEAT: mode = 3; break;
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

        b[0] = 0xA5;
        b[1] = (s.power ? 0x01 : 0x00) | (((temp - 16) & 0x0F) << 4);
        b[2] = 0x20 | ((s.swingV ? 0x01 : 0x00) << 6);   // unknown bit = 1
        b[3] = 0x00;
        b[4] = 12 << 0;                                    // default off hours
        b[5] = (fan & 0x03) << 6;
        b[6] = (mode & 0x07) << 5;
        b[7] = s.sleep ? 0x40 : 0x00;
        int sum = 0;
        for (int i = 0; i < 8; i++) sum += b[i];
        b[8] = sum & 0xFF;

        byte[] data = new byte[9];
        for (int i = 0; i < 9; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR).space(HDR);
        p.mark(HDR).space(HDR_GAP);
        p.bytesMSB(data, 0, 9, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(MIN_GAP);
        return p.build(38000);
    }
}
