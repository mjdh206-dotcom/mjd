package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Toshiba (توشيبا) - 9 byte / 72 bit protocol. */
public class ToshibaProtocol extends Protocol {
    private static final int HDR_MARK = 4400, HDR_SPACE = 4300;
    private static final int BIT_MARK = 580, ONE_SPACE = 1600, ZERO_SPACE = 490;
    private static final int GAP = 7400;

    public ToshibaProtocol() {
        super("toshiba", "توشيبا", "Toshiba", 17, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = new int[9];
        b[0] = 0x0D;  // inverted pair
        b[1] = 0xF2;
        b[2] = 0xFC;  // inverted length (len=3)
        b[3] = 0x03;
        b[4] = 0x01;
        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0; break;
            case AcState.MODE_COOL: mode = 1; break;
            case AcState.MODE_DRY: mode = 2; break;
            case AcState.MODE_HEAT: mode = 3; break;
            case AcState.MODE_FAN: mode = 4; break;
            default: mode = 1; break;
        }
        if (!s.power) mode = 7;
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 2; break;
            case AcState.FAN_MED: fan = 3; break;
            case AcState.FAN_HIGH: fan = 5; break;
            default: fan = 0; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);
        b[5] = ((temp - 17) & 0x0F) << 4;   // swing off
        b[6] = (mode & 0x07) | ((fan & 0x07) << 5);
        b[7] = 0x00;
        int sum = 0;
        for (int i = 0; i < 8; i++) sum ^= b[i];
        b[8] = sum & 0xFF;

        byte[] data = new byte[9];
        for (int i = 0; i < 9; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesMSB(data, 0, 9, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        return p.build(38000);
    }
}
