package com.mjd.acremote.ac;

import java.util.ArrayList;
import java.util.List;

/** Registry of all supported A/C protocols. */
public final class Protocols {
    private static final List<Protocol> ALL = new ArrayList<>();

    static {
        ALL.add(new GreeProtocol());
        ALL.add(new MideaProtocol());
        ALL.add(new CoolixProtocol());
        ALL.add(new LgProtocol());
        ALL.add(new SamsungProtocol());
        ALL.add(new TclProtocol());
        ALL.add(new HaierProtocol());
        ALL.add(new ToshibaProtocol());
        ALL.add(new PanasonicProtocol());
        ALL.add(new MitsubishiProtocol());
        ALL.add(new DaikinProtocol());
    }

    private Protocols() {
    }

    public static List<Protocol> all() {
        return ALL;
    }

    public static Protocol byId(String id) {
        for (Protocol p : ALL) if (p.id.equals(id)) return p;
        return ALL.get(0);
    }

    /** Protocols ordered for the smart scanner (most common first). */
    public static List<Protocol> scanOrder() {
        return all();
    }
}
