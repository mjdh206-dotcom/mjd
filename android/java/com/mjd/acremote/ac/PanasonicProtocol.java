package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Panasonic (باناسونيك) - 27 byte protocol, sent in two sections. */
public class PanasonicProtocol extends Protocol {
    private static final int HDR_MARK = 3456, HDR_SPACE = 1728;
    private static final int BIT_MARK = 432, ONE_SPACE = 1296, ZERO_SPACE = 432;
    private static final int SECTION_GAP = 10000, MESSAGE_GAP = 100000;

    private static final int[] RESET = {
            0x02, 0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x06, 0x02,
            0x20, 0xE0, 0x04, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00,
            0x00, 0x0E, 0xE0, 0x00, 0x00, 0x81, 0x00, 0x00, 0x00};

    public PanasonicProtocol() {
        super("panasonic", "باناسونيك", "Panasonic", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = RESET.clone();
        int mode;
        switch (s.mode) {
            case AcState.MODE_AUTO: mode = 0; break;
            case AcState.MODE_DRY: mode = 2; break;
            case AcState.MODE_COOL: mode = 3; break;
            case AcState.MODE_HEAT: mode = 4; break;
            default: mode = 6; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 1; break;
            case AcState.FAN_MED: fan = 2; break;
            case AcState.FAN_HIGH: fan = 4; break;
            default: fan = 7; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);
        if (s.mode == AcState.MODE_FAN) temp = 27;

        b[13] = (mode << 4) | (s.power ? 0x01 : 0x00);
        b[14] = (b[14] & 0xC0) | (temp & 0x1F);
        int swingV = s.swingV ? 0x0F : 0x03;
        b[16] = ((fan + 3) & 0x0F) << 4 | (swingV & 0x0F);
        b[17] = (b[17] & 0xF0) | (s.swingH ? 0x0D : 0x06);
        int sum = 0xF4;
        for (int i = 0; i < 26; i++) sum += b[i];
        b[26] = sum & 0xFF;

        byte[] data = new byte[27];
        for (int i = 0; i < 27; i++) data[i] = (byte) b[i];

        PulseBuilder p = new PulseBuilder();
        // Section 1: 8 bytes
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesLSB(data, 0, 8, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(SECTION_GAP);
        // Section 2: the rest
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bytesLSB(data, 8, 19, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(MESSAGE_GAP);
        return p.build(36700);
    }
}
