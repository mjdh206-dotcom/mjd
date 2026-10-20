package com.mjd.acremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mjd.acremote.ac.AcState;
import com.mjd.acremote.ac.Protocol;
import com.mjd.acremote.ac.Protocols;
import com.mjd.acremote.data.Profile;
import com.mjd.acremote.data.Store;
import com.mjd.acremote.ir.IrBlaster;

/** Handles the sleep timer firing. */
public class Receiver extends BroadcastReceiver {
    public static final String ACTION_TIMER = "com.mjd.acremote.ACTION_TIMER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TIMER.equals(intent.getAction())) return;
        final PendingResult result = goAsync();
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                handle(app);
            } finally {
                result.finish();
            }
        }, "ir-timer").start();
    }

    private void handle(Context context) {
        Store store = new Store(context);
        Profile profile = store.active();
        if (profile == null) return;
        Protocol protocol = Protocols.byId(profile.protocolId);
        AcState state = profile.toState();
        state.power = false;
        new IrBlaster(context).transmit(protocol.encode(state));
        store.setTimerAt(0);
        store.setLastOffMinutes(0);
        profile.power = false;
        store.upsert(profile);
    }
}
