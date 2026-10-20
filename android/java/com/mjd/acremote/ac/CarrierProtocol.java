package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** Carrier 64-bit (كاريير). */
public class CarrierProtocol extends Protocol {
    private static final int HDR_MARK = 8940, HDR_SPACE = 4556;
    private static final int BIT_MARK = 503, ONE_SPACE = 1736, ZERO_SPACE = 615;
    private static final int GAP = 100000;

    private static final int[] RESET = {0x10, 0x90, 0x00, 0x00, 0x2C, 0x2A, 0x55, 0x84};

    public CarrierProtocol() {
        super("carrier", "كاريير", "Carrier", 16, 30, true, false, false, true);
    }

    @Override
    public Signal encode(AcState s) {
        int[] b = RESET.clone();
        int mode;
        switch (s.mode) {
            case AcState.MODE_HEAT: mode = 1; break;
            case AcState.MODE_FAN: mode = 3; break;
            default: mode = 2; break;              // cool (Carrier has no auto/dry)
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 1; break;
            case AcState.FAN_MED: fan = 2; break;
            case AcState.FAN_HIGH: fan = 3; break;
            default: fan = 0; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);

        b[3] = (s.power ? 0x10 : 0x00);
        b[4] = ((temp - 16) & 0x0F) | (s.swingV ? 0x20 : 0x00);
        b[5] = ((mode & 0x03) << 4) | ((fan & 0x03) << 6);

        long raw = 0;
        for (int i = 0; i < 8; i++) raw = (raw << 8) | (b[i] & 0xFFL);
        // 4-bit checksum of every nibble above bit 20.
        int sum = 0;
        for (int i = 0; i < 11; i++) sum += (raw >>> (20 + i * 4)) & 0x0F;
        raw = (raw & ~0x000F0000L) | (((long) (sum & 0x0F)) << 16);
        b[5] = (b[5] & 0xF0) | (sum & 0x0F);

        debugRaw = b.clone();
        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        // The library sends this 64-bit word least-significant bit first.
        p.bitsMSB(Long.reverse(raw), 64, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(GAP);
        return p.build(38000);
    }
}
