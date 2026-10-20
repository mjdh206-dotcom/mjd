package com.mjd.acremote.ac;

import com.mjd.acremote.ir.PulseBuilder;
import com.mjd.acremote.ir.Signal;

/**
 * Midea (ميديا) - 48 bit protocol, sent as data + inverted data.
 *
 * Byte layout (wire order, verified against IRremoteESP8266):
 *   0: 0xA1                (type 1 + header 0b10100)
 *   1: power | sleep | fan | mode
 *   2: 0x60 | (Fahrenheit - 62)
 *   3: 0xFF                (timer / sensor disabled)
 *   4: 0xFF
 *   5: checksum            (bit-reversed two's complement of reversed bytes 0..4)
 */
public class MideaProtocol extends Protocol {
    private static final int TICK = 80;
    private static final int HDR_MARK = 56 * TICK;   // 4480
    private static final int HDR_SPACE = 56 * TICK;
    private static final int BIT_MARK = 7 * TICK;    // 560
    private static final int ONE_SPACE = 21 * TICK;  // 1680
    private static final int ZERO_SPACE = 7 * TICK;  // 560
    private static final int MIN_GAP = 70 * TICK;    // 5600

    public MideaProtocol() {
        super("midea", "ميديا", "Midea", 17, 30, true, true, true, true);
    }

    @Override
    public Signal encode(AcState s) {
        int mode;
        switch (s.mode) {
            case AcState.MODE_DRY: mode = 1; break;
            case AcState.MODE_AUTO: mode = 2; break;
            case AcState.MODE_HEAT: mode = 3; break;
            case AcState.MODE_FAN: mode = 4; break;
            default: mode = 0; break;
        }
        int fan;
        switch (s.fan) {
            case AcState.FAN_LOW: fan = 1; break;
            case AcState.FAN_MED: fan = 2; break;
            case AcState.FAN_HIGH: fan = 3; break;
            default: fan = 0; break;
        }
        int temp = clamp(s.temp, minTemp, maxTemp);
        int fahrenheit = (temp * 9) / 5 + 32;          // same rounding as the library
        int tempField = clamp(fahrenheit - 62, 0, 24);

        int[] b = new int[6];
        b[0] = 0xA1;
        b[1] = (s.power ? 0x80 : 0x00) | (s.sleep ? 0x40 : 0x00)
                | ((fan & 0x03) << 3) | (mode & 0x07);
        b[2] = 0x60 | tempField;
        b[3] = 0xFF;
        b[4] = 0xFF;

        int sum = 0;
        for (int i = 0; i < 5; i++) sum += reverse8(b[i]);
        b[5] = reverse8((0x100 - (sum & 0xFF)) & 0xFF);

        debugRaw = b.clone();
        byte[] data = new byte[6];
        for (int i = 0; i < 6; i++) data[i] = (byte) b[i];
        byte[] inv = new byte[6];
        for (int i = 0; i < 6; i++) inv[i] = (byte) ~data[i];

        PulseBuilder p = new PulseBuilder();
        for (int phase = 0; phase < 2; phase++) {
            byte[] d = phase == 0 ? data : inv;
            p.mark(HDR_MARK).space(HDR_SPACE);
            p.bytesMSB(d, 0, 6, BIT_MARK, ONE_SPACE, ZERO_SPACE);
            p.mark(BIT_MARK).space(MIN_GAP);
        }
        return p.build(38000);
    }

    static int reverse8(int v) {
        int out = 0;
        for (int i = 0; i < 8; i++) {
            out = (out << 1) | ((v >> i) & 1);
        }
        return out & 0xFF;
    }
}
