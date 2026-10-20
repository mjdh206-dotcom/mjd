package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** TCL 112 / TAC-09CHSD (تي سي إل) - 14 byte protocol. */
public class TclProtocol extends Protocol {
    private static final int HDR_MARK = 3000, HDR_SPACE = 1650;
    private static final int BIT_MARK = 500, ONE_SPACE = 1050, ZERO_SPACE = 325;
    private static final int GAP = 100000;

    private static final int[] RESET = {
            0x23, 0xCB, 0x26, 0x01, 0x00, 0x24, 0x03, 0x07,
            0x40, 0x00, 0x00, 0x00, 0x00, 0x03};

    public TclProtocol() {
        super("tcl", "تي سي إل", "TCL", 16, 31, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = RESET.clone();
        int mode;
        switch (s.mode) {
            case AcState.MODE_HEAT: mode = 1; break;
            case AcState.MODE_DRY: mode = 2; break;
            case AcState.MODE_COOL: mode = 3; break;
            case AcState.MODE_FAN: mode = 7; break;
            default: mode = 8; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 2; break;
            case AcState.FAN_MED: fan = 3; break;
            case AcState.FAN_HIGH: fan = 5; break;
            default: fan = 0; break;
        }
        int temp = clamp(s.temp, minTemp, 30);

        b[5] = (b[5] & ~0x04) | (s.power ? 0x04 : 0);
        if (s.light) b[5] |= 0x40; else b[5] &= ~0x40;
        if (s.econo) b[5] |= 0x80; else b[5] &= ~0x80;
        b[6] = (b[6] & 0xF0) | (mode & 0x0F);
        if (s.turbo) b[6] |= 0x20; else b[6] &= ~0x20;
        b[7] = (b[7] & 0xF0) | ((temp - 16) & 0x0F);
        b[8] = (fan & 0x07) | ((s.swingV ? 0b111 : 0b000) << 3);
        b[12] = 0x80;
        if (s.swingH) b[12] |= 0x08;

        int sum = 0;
        for (int i = 0; i < 13; i++) sum += b[i] & 0xFF;
        if (b[3] == 0x02) sum += 0x0F;
        b[13] = sum & 0xFF;

        byte[] data = new byte[14];
        for (int i = 0; i < 14; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesLSB(data, 0, 14, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        return p.build(38000);
    }
}
