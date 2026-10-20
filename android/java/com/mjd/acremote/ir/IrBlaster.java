package com.mjd.acremote.ir;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Build;

/** Sends IR patterns through the phone's IR emitter. */
public final class IrBlaster {
    private final ConsumerIrManager manager;

    public IrBlaster(Context context) {
        ConsumerIrManager m = null;
        try {
            m = (ConsumerIrManager) context.getApplicationContext()
                    .getSystemService(Context.CONSUMER_IR_SERVICE);
        } catch (Throwable ignored) {
        }
        manager = m;
    }

    public boolean hasEmitter() {
        try {
            return manager != null && manager.hasIrEmitter();
        } catch (Throwable t) {
            return false;
        }
    }

    public String describeHardware() {
        if (manager == null) return "لا يوجد مستقبل أشعة تحت الحمراء";
        try {
            if (!manager.hasIrEmitter()) return "الهاتف لا يدعم الأشعة تحت الحمراء";
            ConsumerIrManager.CarrierFrequencyRange[] ranges = manager.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) return "مدعوم";
            StringBuilder sb = new StringBuilder();
            for (ConsumerIrManager.CarrierFrequencyRange r : ranges) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(r.getMinFrequency()).append("-").append(r.getMaxFrequency()).append(" Hz");
            }
            return sb.toString();
        } catch (Throwable t) {
            return "مدعوم";
        }
    }

    /** Splits very long patterns on large gaps so every driver can handle them. */
    public boolean transmit(Signal signal) {
        if (manager == null || signal == null) return false;
        int[] pattern = signal.pattern;
        try {
            if (pattern.length <= 320) {
                manager.transmit(signal.frequency, pattern);
                return true;
            }
            int start = 0;
            while (start < pattern.length) {
                int end = start;
                int length = 0;
                while (end < pattern.length) {
                    if (end > start + 40 && end % 2 == 1 && pattern[end] > 15000) break;
                    length++;
                    end++;
                }
                if (end == start) break;
                int[] chunk = new int[end - start];
                System.arraycopy(pattern, start, chunk, 0, chunk.length);
                if (chunk.length % 2 == 1 && chunk.length > 1) {
                    // Keep the pattern alternating: drop a trailing mark.
                    int[] tmp = new int[chunk.length - 1];
                    System.arraycopy(chunk, 0, tmp, 0, tmp.length);
                    chunk = tmp;
                }
                manager.transmit(signal.frequency, chunk);
                start = end;
                if (start < pattern.length) {
                    Thread.sleep(Math.min(120, Math.max(20, pattern[start - 1] / 1000L)));
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.contains("generic") || Build.PRODUCT.contains("sdk");
    }
}
