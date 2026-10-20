package com.mjd.acremote.ac;

import com.mjd.acremote.ir.Signal;

/** Base class for an A/C IR protocol encoder. */
public abstract class Protocol {
    public final String id;
    public final String nameAr;
    public final String nameEn;
    public final int minTemp;
    public final int maxTemp;
    public final boolean supportsHeat;
    public final boolean supportsAuto;
    public final boolean supportsDry;
    public final boolean supportsFan;

    protected Protocol(String id, String nameAr, String nameEn, int minTemp, int maxTemp,
                       boolean supportsHeat, boolean supportsAuto, boolean supportsDry,
                       boolean supportsFan) {
        this.id = id;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.supportsHeat = supportsHeat;
        this.supportsAuto = supportsAuto;
        this.supportsDry = supportsDry;
        this.supportsFan = supportsFan;
    }

    /** Selected encoding variant (some brands use several code formats). */
    public int variant = 0;

    public abstract Signal encode(AcState state);

    public AcState defaultState() {
        AcState s = new AcState();
        s.power = true;
        s.mode = AcState.MODE_COOL;
        s.temp = 24;
        s.fan = AcState.FAN_AUTO;
        return s;
    }

    /** A safe "power" test message used by the smart scanner. */
    public Signal testSignal() {
        return encode(defaultState());
    }

    /** Some protocols have more than one encoding variant worth trying. */
    public int variantCount() {
        return 1;
    }

    public Signal testSignal(int variant) {
        int previous = this.variant;
        this.variant = variant;
        Signal sig = encode(defaultState());
        this.variant = previous;
        return sig;
    }

    protected static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** Repeats a signal payload a number of times separated by a gap. */
    protected static Signal repeat(Signal base, int times, int gapUs) {
        if (times <= 1) return base;
        int[] out = new int[base.pattern.length * times];
        int n = 0;
        for (int r = 0; r < times; r++) {
            for (int v : base.pattern) out[n++] = v;
            if (r + 1 < times) {
                // Merge the trailing space with the inter-message gap.
                out[n - 1] = out[n - 1] + gapUs;
            }
        }
        int[] trimmed = new int[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return new Signal(base.frequency, trimmed);
    }
}
