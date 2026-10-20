package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Electra (إلكترا) - 13 byte protocol. */
public class ElectraProtocol extends Protocol {
    private static final int HDR_MARK = 9166, HDR_SPACE = 4470;
    private static final int BIT_MARK = 646, ONE_SPACE = 1647, ZERO_SPACE = 547;
    private static final int GAP = 100000;

    public ElectraProtocol() {
        super("electra", "إلكترا", "Electra", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = new int[13];
        b[0] = 0xC3;
        b[11] = 0x08;

        int mode;
        switch (s.mode) {
            case AcState.MODE_COOL: mode = 1; break;
            case AcState.MODE_DRY: mode = 2; break;
            case AcState.MODE_HEAT: mode = 4; break;
            case AcState.MODE_FAN: mode = 6; break;
            default: mode = 0; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 3; break;
            case AcState.FAN_MED: fan = 2; break;
            case AcState.FAN_HIGH: fan = 1; break;
            default: fan = 5; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);

        b[1] = (s.swingV ? 0 : 7) | (((temp - 8) & 0x1F) << 3);
        b[2] = (s.swingH ? 0 : 7) << 5;
        b[4] = (fan & 0x07) << 5;
        b[5] = (s.turbo ? 0x40 : 0) | (s.sleep ? 0x80 : 0);
        b[6] = (mode & 0x07) << 5;
        b[9] = s.power ? 0x20 : 0x00;

        int sum = 0;
        for (int i = 0; i < 12; i++) sum += b[i];
        b[12] = sum & 0xFF;

        byte[] data = new byte[13];
        for (int i = 0; i < 13; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesLSB(data, 0, 13, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        return p.build(38000);
    }
}
