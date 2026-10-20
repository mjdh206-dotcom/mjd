package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Mitsubishi Electric MSZ (ميتسوبيشي) - 18 byte / 144 bit protocol. */
public class MitsubishiProtocol extends Protocol {
    private static final int HDR_MARK = 3400, HDR_SPACE = 1750;
    private static final int BIT_MARK = 450, ONE_SPACE = 1300, ZERO_SPACE = 420;
    private static final int RPT_MARK = 440, RPT_SPACE = 15500;

    private static final int[] RESET = {
            0x23, 0xCB, 0x26, 0x01, 0x00, 0x20, 0x08, 0x06,
            0x30, 0x45, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public MitsubishiProtocol() {
        super("mitsubishi", "ميتسوبيشي", "Mitsubishi", 16, 31, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = RESET.clone();
        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0b100; break;
            case AcState.MODE_COOL: mode = 0b011; break;
            case AcState.MODE_DRY: mode = 0b010; break;
            case AcState.MODE_HEAT: mode = 0b001; break;
            default: mode = 0b111; break;
        }
        int temp = clamp(s.temp, minTemp, 31);
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 3; break;
            case AcState.FAN_MED: fan = 2; break;
            case AcState.FAN_HIGH: fan = 4; break;
            default: fan = 0; break;
        }
        int vane = s.swingV ? 0b111 : 0b000;

        b[5] = (b[5] & 0xDF) | (s.power ? 0x20 : 0x00);
        b[6] = (b[6] & 0xC7) | ((mode & 0x07) << 3);
        switch (s.mode) {
            case AcState.MODE_AUTO: case AcState.MODE_HEAT: b[8] = 0x30; break;
            case AcState.MODE_COOL: b[8] = 0x36; break;
            case AcState.MODE_DRY: b[8] = 0x32; break;
            case AcState.MODE_FAN: b[8] = 0x37; break;
        }
        b[7] = (b[7] & 0xF0) | ((temp - 16) & 0x0F);
        b[9] = 0x40 | (fan & 0x07) | ((vane & 0x07) << 3) | (fan == 0 ? 0x80 : 0x00);
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += b[i];
        b[17] = sum & 0xFF;

        debugRaw = b.clone();
        byte[] data = new byte[18];
        for (int i = 0; i < 18; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        for (int rep = 0; rep < 2; rep++) {
            p.mark(HDR_MARK).space(HDR_SPACE);
            p.bytesLSB(data, 0, 18, BIT_MARK, ONE_SPACE, ZERO_SPACE);
            p.mark(RPT_MARK).space(RPT_SPACE);
        }
        return p.build(38000);
    }
}
