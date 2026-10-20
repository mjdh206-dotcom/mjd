package com.mjd.acremote.ir;

/** An IR message: carrier frequency (Hz) + alternating on/off durations in microseconds. */
public final class Signal {
    public final int frequency;
    public final int[] pattern;

    public Signal(int frequency, int[] pattern) {
        this.frequency = frequency;
        this.pattern = pattern;
    }

    public int durationUs() {
        long total = 0;
        for (int v : pattern) total += v;
        return (int) total;
    }
}
