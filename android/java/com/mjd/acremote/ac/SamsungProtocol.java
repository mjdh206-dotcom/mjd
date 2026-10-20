package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Samsung (سامسونج) A/C - 14 byte state sent in two 7-byte sections. */
public class SamsungProtocol extends Protocol {
    private static final int HDR_MARK = 690, HDR_SPACE = 17844;
    private static final int SECTION_MARK = 3086, SECTION_SPACE = 8864;
    private static final int BIT_MARK = 586, ONE_SPACE = 1432, ZERO_SPACE = 436;
    private static final int SECTION_GAP = 2886;
    private static final int MESSAGE_GAP = 100000;

    /** Special extended messages used to switch the unit on/off. */
    private static final int[] EXT_ON = {
            0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
            0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
            0x01, 0xE2, 0xFE, 0x71, 0x80, 0x11, 0xF0};
    private static final int[] EXT_OFF = {
            0x02, 0xB2, 0x0F, 0x00, 0x00, 0x00, 0xC0,
            0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
            0x01, 0x02, 0xFF, 0x71, 0x80, 0x11, 0xC0};

    private boolean wasPower = true;

    private static final int[] RESET = {
            0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
            0x01, 0x02, 0xAE, 0x71, 0x00, 0x15, 0xF0};

    public SamsungProtocol() {
        super("samsung", "سامسونج", "Samsung", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        if (!s.power) {
            wasPower = false;
            return frame(EXT_OFF, EXT_OFF.length, MESSAGE_GAP, false);
        }
        boolean justTurnedOn = !wasPower;
        wasPower = true;
        int[] b = RESET.clone();
        int temp = clamp(s.temp, minTemp, maxTemp);
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
            case AcState.FAN_LOW: fan = 2; break;
            case AcState.FAN_MED: fan = 4; break;
            case AcState.FAN_HIGH: fan = 5; break;
            default: fan = (mode == 0) ? 6 : 0; break;
        }
        int powerBits = s.power ? 0b11 : 0b00;

        b[6] = (b[6] & 0xCF) | (powerBits << 4);
        b[7] = 0x01;
        b[13] = (b[13] & 0xCF) | (powerBits << 4);
        b[11] = (b[11] & 0x0F) | (((temp - 16) & 0x0F) << 4);
        b[12] = (fan & 0x07) << 1 | ((mode & 0x07) << 4);
        b[9] = (b[9] & 0x0F) | ((s.swingV ? 0b011 : 0b000) << 4);
        if (s.turbo) b[10] = (b[10] & 0xF1) | (0b001 << 1);
        if (s.sleep) b[5] |= 0x10;
        b[5] = (b[5] & 0xEF) | (s.sleep ? 0x10 : 0);

        // Checksums for the two sections.
        fixSection(b, 0);
        fixSection(b, 7);

        Signal standard = frame(b, 14, MESSAGE_GAP, true);
        if (!justTurnedOn) return standard;
        // When powering on from a known-off state, the unit expects the special
        // extended "on" message first.
        Signal on = frame(EXT_ON, EXT_ON.length, 20000, false);
        int[] combined = new int[on.pattern.length + standard.pattern.length];
        System.arraycopy(on.pattern, 0, combined, 0, on.pattern.length);
        System.arraycopy(standard.pattern, 0, combined, on.pattern.length,
                standard.pattern.length);
        return new Signal(38000, combined);
    }

    private static Signal frame(int[] bytes, int length, int tailGap, boolean fixSums) {
        int[] data = new int[length];
        System.arraycopy(bytes, 0, data, 0, length);
        if (fixSums) {
            fixSection(data, 0);
            fixSection(data, 7);
        }
        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        for (int sec = 0; sec * 7 < length; sec++) {
            int off = sec * 7;
            int len = Math.min(7, length - off);
            p.mark(SECTION_MARK).space(SECTION_SPACE);
            for (int i = 0; i < len; i++) {
                p.bitsLSB(data[off + i] & 0xFF, 8, BIT_MARK, ONE_SPACE, ZERO_SPACE);
            }
            p.mark(BIT_MARK).space(SECTION_GAP);
        }
        p.space(tailGap - SECTION_GAP);
        return p.build(38000);
    }

    private static void fixSection(int[] b, int off) {
        int sum = 0;
        sum += Integer.bitCount(b[off] & 0xFF);
        sum += Integer.bitCount(b[off + 1] & 0x0F);
        sum += Integer.bitCount((b[off + 2] >> 4) & 0x0F);
        for (int i = 3; i < 7; i++) sum += Integer.bitCount(b[off + i] & 0xFF);
        sum ^= 0xFF;
        b[off + 2] = (b[off + 2] & 0xF0) | ((sum >> 4) & 0x0F);
        b[off + 1] = (b[off + 1] & 0x0F) | ((sum & 0x0F) << 4);
    }
}
