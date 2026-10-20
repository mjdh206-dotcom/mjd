package com.mjd.acremote.ir;

/** Helper to build IR pulse patterns. */
public final class PulseBuilder {
    private int[] p = new int[512];
    private int n;

    public PulseBuilder mark(int us) {
        if (us > 0) add(us);
        return this;
    }

    public PulseBuilder space(int us) {
        if (us > 0) add(us);
        return this;
    }

    private void add(int us) {
        if (n == p.length) {
            int[] q = new int[n * 2];
            System.arraycopy(p, 0, q, 0, n);
            p = q;
        }
        p[n++] = us;
    }

    public int size() {
        return n;
    }

    public PulseBuilder bitsMSB(long data, int nbits,
                                int bitMark, int oneSpace, int zeroSpace) {
        for (int i = nbits - 1; i >= 0; i--) {
            mark(bitMark);
            space(((data >>> i) & 1L) != 0 ? oneSpace : zeroSpace);
        }
        return this;
    }

    public PulseBuilder bitsLSB(long data, int nbits,
                                int bitMark, int oneSpace, int zeroSpace) {
        for (int i = 0; i < nbits; i++) {
            mark(bitMark);
            space(((data >>> i) & 1L) != 0 ? oneSpace : zeroSpace);
        }
        return this;
    }

    public PulseBuilder bytesMSB(byte[] data, int offset, int length,
                                 int bitMark, int oneSpace, int zeroSpace) {
        for (int i = 0; i < length; i++) bitsMSB(data[offset + i] & 0xFF, 8, bitMark, oneSpace, zeroSpace);
        return this;
    }

    public PulseBuilder bytesLSB(byte[] data, int offset, int length,
                                 int bitMark, int oneSpace, int zeroSpace) {
        for (int i = 0; i < length; i++) bitsLSB(data[offset + i] & 0xFF, 8, bitMark, oneSpace, zeroSpace);
        return this;
    }

    public Signal build(int frequencyHz) {
        int[] out = new int[n];
        System.arraycopy(p, 0, out, 0, n);
        return new Signal(frequencyHz, out);
    }
}
