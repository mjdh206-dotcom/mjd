package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/** LG (إل جي) - 28 bit protocol. */
public class LgProtocol extends Protocol {
    private static final int HDR_MARK = 8500, HDR_SPACE = 4250;
    private static final int BIT_MARK = 550, ONE_SPACE = 1600, ZERO_SPACE = 550;
    private static final int MIN_GAP = 39750, RPT_SPACE = 2250;
    private static final int SIGN = 0x88;

    public LgProtocol() {
        super("lg", "إل جي", "LG", 16, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int mode;
        switch (s.mode) {
            case AcState.MODE_DRY: mode = 1; break;
            case AcState.MODE_FAN: mode = 2; break;
            case AcState.MODE_AUTO: mode = 3; break;
            case AcState.MODE_HEAT: mode = 4; break;
            default: mode = 0; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 1; break;
            case AcState.FAN_MED: fan = 2; break;
            case AcState.FAN_HIGH: fan = 4; break;
            default: fan = 5; break;
        }
        int temp = s.power ? (clamp(s.temp, minTemp, maxTemp) - 15) : 0;
        int power = s.power ? 0 : 3;

        int raw = (SIGN << 20) | ((power & 0x03) << 18) | ((mode & 0x07) << 12)
                | ((temp & 0x0F) << 8) | ((fan & 0x0F) << 4);
        int sum = 0;
        int v = raw >> 4;
        for (int i = 0; i < 4; i++) {
            sum += v & 0x0F;
            v >>= 4;
        }
        raw |= (sum & 0x0F);

        PulseBuilder p = new PulseBuilder();
        p.mark(HDR_MARK).space(HDR_SPACE);
        p.bitsMSB(raw & 0x0FFFFFFFL, 28, BIT_MARK, ONE_SPACE, ZERO_SPACE);
        p.mark(BIT_MARK).space(MIN_GAP);
        // Mandatory repeat message.
        p.mark(HDR_MARK).space(RPT_SPACE);
        p.mark(BIT_MARK).space(MIN_GAP);
        return p.build(38000);
    }
}
