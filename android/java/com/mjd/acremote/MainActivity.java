package com.mjd.acremote;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mjd.acremote.ac.AcState;
import com.mjd.acremote.ac.Protocol;
import com.mjd.acremote.ac.Protocols;
import com.mjd.acremote.data.Profile;
import com.mjd.acremote.data.Store;
import com.mjd.acremote.ir.IrBlaster;
import com.mjd.acremote.ir.Signal;
import com.mjd.acremote.ui.BackdropView;
import com.mjd.acremote.ui.PowerButtonView;
import com.mjd.acremote.ui.TempRingView;
import com.mjd.acremote.ui.Ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Main remote-control screen. */
public class MainActivity extends Activity {
    private Store store;
    private Profile profile;
    private Protocol protocol;
    private IrBlaster blaster;

    private TempRingView ring;
    private PowerButtonView powerButton;
    private TextView subtitle;
    private TextView statusLabel;
    private TextView toastView;
    private TextView timerValue;
    private LinearLayout modesRow;
    private LinearLayout fansRow;
    private LinearLayout extrasGrid;
    private TextView tempValueView;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        store = new Store(this);
        blaster = new IrBlaster(this);
        if (store.active() == null) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        setContentView(buildRoot());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (store == null) return;
        Profile active = store.active();
        if (active == null) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        profile = active;
        protocol = Protocols.byId(profile.protocolId);
        protocol.variant = profile.variant;
        refreshAll();
        updateTimerLabel();
        tickTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    // ---------------------------------------------------------------- UI ----

    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.addView(new BackdropView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        content.addView(header());

        content.addView(space(16));

        // --- main dial card -------------------------------------------------
        LinearLayout dialCard = Ui.column(this);
        dialCard.setBackground(Ui.gradient(0xFF111D31, 0xFF0D1727, 30, Ui.STROKE, 1, this));
        dialCard.setPadding(Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 18));
        dialCard.setElevation(Ui.dp(this, 8));

        statusLabel = Ui.text(this, "", 12f, Ui.ACCENT, true);
        statusLabel.setPadding(0, 0, 0, 0);
        dialCard.addView(statusLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 22)));

        ring = new TempRingView(this);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 300));
        dialCard.addView(ring, ringLp);

        LinearLayout controls = Ui.row(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);

        TextView minus = roundButton("−");
        minus.setOnClickListener(v -> changeTemp(-1));
        LinearLayout.LayoutParams minusLp = new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62));
        minusLp.setMarginEnd(Ui.dp(this, 22));

        powerButton = new PowerButtonView(this);
        powerButton.setOnClickListener(v -> togglePower());
        LinearLayout.LayoutParams powerLp = new LinearLayout.LayoutParams(Ui.dp(this, 82), Ui.dp(this, 82));
        powerLp.setMarginEnd(Ui.dp(this, 22));

        TextView plus = roundButton("+");
        plus.setOnClickListener(v -> changeTemp(1));

        controls.addView(minus, minusLp);
        controls.addView(powerButton, powerLp);
        controls.addView(plus, new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62)));
        dialCard.addView(controls);

        content.addView(dialCard);
        content.addView(space(20));

        // --- modes ----------------------------------------------------------
        content.addView(sectionTitle("وضع التشغيل", "MODE"));
        modesRow = Ui.row(this);
        modesRow.setPadding(0, Ui.dp(this, 10), 0, 0);
        content.addView(modesRow);
        content.addView(space(20));

        // --- fan ------------------------------------------------------------
        content.addView(sectionTitle("سرعة المروحة", "FAN"));
        fansRow = Ui.row(this);
        fansRow.setPadding(0, Ui.dp(this, 10), 0, 0);
        content.addView(fansRow);
        content.addView(space(20));

        // --- extras ---------------------------------------------------------
        content.addView(sectionTitle("وظائف إضافية", "EXTRAS"));
        extrasGrid = Ui.column(this);
        extrasGrid.setPadding(0, Ui.dp(this, 10), 0, 0);
        content.addView(extrasGrid);
        content.addView(space(20));

        // --- timer ----------------------------------------------------------
        content.addView(timerCard());
        content.addView(space(22));

        TextView footer = Ui.text(this,
                "يعمل عبر باعث الأشعة تحت الحمراء المدمج في الهاتف.\nوجّه الهاتف نحو المكيف أثناء الإرسال.",
                11.5f, 0xFF63799A, false);
        footer.setLineSpacing(Ui.dp(this, 3), 1f);
        content.addView(footer);

        // floating toast
        toastView = Ui.text(this, "", 13f, Color.WHITE, true);
        toastView.setBackground(Ui.round(0xEE12283A, 22, 0x5535E0C8, 1, this));
        toastView.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        toastView.setAlpha(0f);
        FrameLayout.LayoutParams toastLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toastLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        toastLp.topMargin = Ui.dp(this, 26);
        root.addView(toastView, toastLp);

        return root;
    }

    private View header() {
        LinearLayout header = Ui.row(this);

        LinearLayout titles = Ui.column(this);
        TextView title = Ui.text(this, "ريموت برو", 22f, Ui.TEXT, true);
        title.setGravity(Gravity.START);
        subtitle = Ui.text(this, "", 12f, Ui.MUTED, false);
        subtitle.setGravity(Gravity.START);
        subtitle.setPadding(0, Ui.dp(this, 2), 0, 0);
        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, Ui.weight(1f));

        TextView devices = iconChip("▤");
        devices.setOnClickListener(v -> showDevices());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44));
        dlp.setMarginEnd(Ui.dp(this, 10));
        header.addView(devices, dlp);

        TextView add = iconChip("+");
        add.setTextSize(22f);
        add.setOnClickListener(v -> startActivity(new Intent(this, SetupActivity.class)));
        header.addView(add, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        return header;
    }

    private TextView iconChip(String glyph) {
        TextView v = Ui.text(this, glyph, 18f, Ui.TEXT, true);
        v.setBackground(Ui.round(Ui.CARD, 15, Ui.STROKE, 1, this));
        v.setClickable(true);
        return v;
    }

    private TextView roundButton(String glyph) {
        TextView v = Ui.text(this, glyph, 30f, Ui.TEXT, false);
        v.setBackground(Ui.round(0xFF16243A, 31, Ui.STROKE, 1, this));
        v.setClickable(true);
        v.setElevation(Ui.dp(this, 3));
        return v;
    }

    private View sectionTitle(String ar, String en) {
        LinearLayout row = Ui.row(this);
        TextView t = Ui.text(this, ar, 15f, Ui.TEXT, true);
        t.setGravity(Gravity.START);
        row.addView(t, Ui.weight(1f));
        TextView tag = Ui.text(this, en, 10f, 0xFF4C688C, true);
        tag.setLetterSpacing(0.12f);
        row.addView(tag);
        return row;
    }

    private View timerCard() {
        LinearLayout card = Ui.row(this);
        card.setBackground(Ui.gradient(0xFF111D31, 0xFF0E1829, 22, Ui.STROKE, 1, this));
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));

        LinearLayout texts = Ui.column(this);
        TextView t = Ui.text(this, "مؤقت الإطفاء", 14f, Ui.TEXT, true);
        t.setGravity(Gravity.START);
        timerValue = Ui.text(this, "غير مضبوط", 12f, Ui.MUTED, false);
        timerValue.setGravity(Gravity.START);
        timerValue.setPadding(0, Ui.dp(this, 3), 0, 0);
        texts.addView(t);
        texts.addView(timerValue);
        card.addView(texts, Ui.weight(1f));

        TextView set = Ui.text(this, "ضبط", 13f, 0xFF04120F, true);
        set.setBackground(Ui.gradient(Ui.ACCENT, Ui.ACCENT, 16, Ui.ACCENT, 0, this));
        set.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 10));
        set.setOnClickListener(v -> showTimerDialog());
        card.addView(set);
        return card;
    }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, Ui.dp(this, dp)));
        return v;
    }

    // ------------------------------------------------------------- render ---

    private void refreshAll() {
        subtitle.setText(protocol.nameAr + "  •  " + profile.name);
        statusLabel.setText(blaster.hasEmitter() ? "●  جاهز للإرسال" : "●  لا يوجد باعث IR");
        statusLabel.setTextColor(blaster.hasEmitter() ? Ui.ACCENT : 0xFFFF7272);
        ring.set(profile.temp, protocol.minTemp, protocol.maxTemp,
                AcState.modeNameAr(profile.mode), profile.power);
        powerButton.setOn(profile.power);
        buildModes();
        buildFans();
        buildExtras();
    }

    private void buildModes() {
        modesRow.removeAllViews();
        String[] names = {"تلقائي", "تبريد", "تجفيف", "مروحة", "تدفئة"};
        String[] icons = {"◌", "❄", "💧", "🌀", "☀"};
        int[] modes = {AcState.MODE_AUTO, AcState.MODE_COOL, AcState.MODE_DRY,
                AcState.MODE_FAN, AcState.MODE_HEAT};
        boolean[] enabled = {protocol.supportsAuto, true, protocol.supportsDry,
                protocol.supportsFan, protocol.supportsHeat};
        for (int i = 0; i < modes.length; i++) {
            final int mode = modes[i];
            boolean active = profile.mode == mode;
            LinearLayout item = tile(icons[i], names[i], active, enabled[i]);
            item.setAlpha(enabled[i] ? 1f : 0.35f);
            if (enabled[i]) item.setOnClickListener(v -> {
                profile.mode = mode;
                send();
                refreshAll();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(Ui.dp(this, 7));
            modesRow.addView(item, lp);
        }
    }

    private void buildFans() {
        fansRow.removeAllViews();
        String[] names = {"تلقائي", "منخفضة", "متوسطة", "عالية"};
        String[] icons = {"A", "1", "2", "3"};
        for (int i = 0; i < names.length; i++) {
            final int fan = i;
            LinearLayout item = tile(icons[i], names[i], profile.fan == fan, true);
            item.setOnClickListener(v -> {
                profile.fan = fan;
                send();
                refreshAll();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(Ui.dp(this, 8));
            fansRow.addView(item, lp);
        }
    }

    private void buildExtras() {
        extrasGrid.removeAllViews();
        addExtraRow(
                extra("تأرجح عمودي", "↕", profile.swingV, () -> {
                    profile.swingV = !profile.swingV;
                    send();
                }),
                extra("تأرجح أفقي", "↔", profile.swingH, () -> {
                    profile.swingH = !profile.swingH;
                    send();
                }));
        extrasGrid.addView(space(8));
        addExtraRow(
                extra("تيربو", "⚡", profile.turbo, () -> {
                    profile.turbo = !profile.turbo;
                    send();
                }),
                extra("سكون", "☾", profile.sleep, () -> {
                    profile.sleep = !profile.sleep;
                    send();
                }));
        extrasGrid.addView(space(8));
        addExtraRow(
                extra("إضاءة الشاشة", "☀", profile.light, () -> {
                    profile.light = !profile.light;
                    send();
                }),
                extra("اقتصادي", "◎", profile.econo, () -> {
                    profile.econo = !profile.econo;
                    send();
                }));
    }

    private void addExtraRow(View a, View b) {
        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(a, lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.setMarginStart(Ui.dp(this, 8));
        row.addView(b, lp2);
        extrasGrid.addView(row);
    }

    private LinearLayout tile(String icon, String label, boolean active, boolean enabled) {
        LinearLayout box = Ui.column(this);
        box.setGravity(Gravity.CENTER);
        box.setPadding(Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12));
        box.setBackground(active
                ? Ui.gradient(0xFF12333A, 0xFF10262F, 18, Ui.ACCENT, 1, this)
                : Ui.round(Ui.CARD, 18, Ui.STROKE, 1, this));
        TextView i = Ui.text(this, icon, 17f, active ? Ui.ACCENT : Ui.TEXT, false);
        TextView l = Ui.text(this, label, 11f, active ? Ui.ACCENT : Ui.MUTED, active);
        l.setPadding(0, Ui.dp(this, 5), 0, 0);
        box.addView(i);
        box.addView(l);
        box.setClickable(enabled);
        return box;
    }

    private View extra(String title, String icon, boolean on, Runnable action) {
        LinearLayout card = Ui.row(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        card.setBackground(on
                ? Ui.gradient(0xFF12333A, 0xFF0F2530, 18, Ui.ACCENT, 1, this)
                : Ui.round(Ui.CARD, 18, Ui.STROKE, 1, this));

        TextView ic = Ui.text(this, icon, 17f, on ? Ui.ACCENT : Ui.TEXT, false);
        ic.setBackground(Ui.round(on ? 0x2235E0C8 : 0xFF17253C, 13, 0, 0, this));
        ic.setPadding(Ui.dp(this, 11), Ui.dp(this, 8), Ui.dp(this, 11), Ui.dp(this, 8));
        card.addView(ic);

        TextView t = Ui.text(this, title, 12.5f, on ? Ui.ACCENT : Ui.TEXT, on);
        t.setGravity(Gravity.START);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMarginStart(Ui.dp(this, 10));
        card.addView(t, tlp);

        TextView dot = Ui.text(this, on ? "●" : "○", 12f, on ? Ui.ACCENT : 0xFF3B5170, false);
        card.addView(dot);

        card.setOnClickListener(v -> {
            action.run();
            refreshAll();
        });
        return card;
    }

    // ------------------------------------------------------------ actions ---

    private void changeTemp(int delta) {
        int t = profile.temp + delta;
        t = Math.max(protocol.minTemp, Math.min(protocol.maxTemp, t));
        if (t == profile.temp) return;
        profile.temp = t;
        send();
        refreshAll();
    }

    private void togglePower() {
        profile.power = !profile.power;
        send();
        refreshAll();
        haptic(18);
    }

    private void send() {
        protocol.variant = profile.variant;
        final Signal signal = protocol.encode(profile.toState());
        store.upsert(profile);
        haptic(10);
        flash(blaster.hasEmitter() ? "تم الإرسال" : "تعذر الإرسال — لا يوجد باعث IR");
        if (!blaster.hasEmitter()) return;
        new Thread(() -> blaster.transmit(signal), "ir-send").start();
    }

    private void flash(String text) {
        toastView.setText(text);
        toastView.animate().cancel();
        toastView.setAlpha(0f);
        toastView.setTranslationY(-Ui.dp(this, 8));
        toastView.animate().alpha(1f).translationY(0).setDuration(160).start();
        handler.postDelayed(() -> toastView.animate().alpha(0f).setDuration(320).start(), 1100);
    }

    private void haptic(int ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) v.vibrate(ms);
        } catch (Throwable ignored) {
        }
    }

    // -------------------------------------------------------------- timer ---

    private void showTimerDialog() {
        final String[] labels = {"إيقاف بعد 30 دقيقة", "إيقاف بعد ساعة", "إيقاف بعد ساعتين",
                "إيقاف بعد 4 ساعات", "إلغاء المؤقت"};
        final int[] minutes = {30, 60, 120, 240, 0};
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = Ui.column(this);
        box.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 8));
        box.setBackground(Ui.gradient(0xFF101B2E, 0xFF0C1626, 24, Ui.STROKE, 1, this));
        TextView title = Ui.text(this, "مؤقت الإطفاء التلقائي", 15f, Ui.TEXT, true);
        title.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 10));
        box.addView(title);
        for (int i = 0; i < labels.length; i++) {
            final int m = minutes[i];
            TextView item = Ui.text(this, labels[i], 14f, i == 4 ? 0xFFFF8A8A : Ui.TEXT, false);
            item.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14));
            item.setBackground(Ui.round(0x00000000, 14, 0, 0, this));
            item.setOnClickListener(v -> {
                setTimer(m);
                d.dismiss();
            });
            box.addView(item);
        }
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(Ui.dp(this, 300), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }

    private void setTimer(int minutes) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, Receiver.class);
        intent.setAction(Receiver.ACTION_TIMER);
        PendingIntent pi = PendingIntent.getBroadcast(this, 7, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
        if (minutes == 0) {
            store.setTimerAt(0);
            store.setLastOffMinutes(0);
            flash("تم إلغاء المؤقت");
            updateTimerLabel();
            return;
        }
        long at = System.currentTimeMillis() + minutes * 60_000L;
        if (am != null) {
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    exact = am.canScheduleExactAlarms();
                } catch (Throwable t) {
                    exact = false;
                }
            }
            try {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (SecurityException e) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        }
        store.setTimerAt(at);
        store.setLastOffMinutes(minutes);
        flash("سيتم إطفاء المكيف تلقائيًا");
        updateTimerLabel();
        tickTimer();
    }

    private void updateTimerLabel() {
        long at = store.timerAt();
        if (at <= System.currentTimeMillis()) {
            timerValue.setText("غير مضبوط");
            return;
        }
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(at));
        timerValue.setText("سيتوقف عند " + time);
    }

    private void tickTimer() {
        handler.removeCallbacksAndMessages(null);
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                updateTimerLabel();
                if (store.timerAt() > System.currentTimeMillis()) {
                    handler.postDelayed(this, 15000);
                }
            }
        };
        handler.postDelayed(r, 15000);
    }

    // ------------------------------------------------------------ devices ---

    private void showDevices() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = Ui.column(this);
        box.setPadding(Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 10));
        box.setBackground(Ui.gradient(0xFF101B2E, 0xFF0C1626, 24, Ui.STROKE, 1, this));

        TextView title = Ui.text(this, "أجهزتي", 16f, Ui.TEXT, true);
        title.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12));
        box.addView(title);

        final List<Profile> profiles = store.profiles();
        for (final Profile p : profiles) {
            Protocol pr = Protocols.byId(p.protocolId);
            LinearLayout row = Ui.row(this);
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
            row.setBackground(Ui.round(p.id.equals(profile.id) ? 0xFF13303A : 0xFF121E31,
                    16, p.id.equals(profile.id) ? 0x5535E0C8 : 0, 1, this));
            LinearLayout texts = Ui.column(this);
            TextView n = Ui.text(this, p.name, 14f, Ui.TEXT, true);
            n.setGravity(Gravity.START);
            TextView b = Ui.text(this, pr.nameAr + " • " + pr.nameEn, 11f, Ui.MUTED, false);
            b.setGravity(Gravity.START);
            texts.addView(n);
            texts.addView(b);
            row.addView(texts, Ui.weight(1f));
            TextView del = Ui.text(this, "✕", 15f, 0xFF6E86A6, false);
            del.setPadding(Ui.dp(this, 12), 0, 0, 0);
            del.setOnClickListener(v -> {
                store.delete(p.id);
                d.dismiss();
                refreshAll();
            });
            row.addView(del);
            row.setOnClickListener(v -> {
                store.setActive(p.id);
                d.dismiss();
                onResume();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(this, 8);
            box.addView(row, lp);
        }

        TextView add = Ui.text(this, "＋  إضافة مكيف جديد", 14f, 0xFF04120F, true);
        add.setBackground(Ui.gradient(Ui.ACCENT, Ui.ACCENT, 16, Ui.ACCENT, 0, this));
        add.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
        add.setOnClickListener(v -> {
            d.dismiss();
            startActivity(new Intent(this, SetupActivity.class));
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = Ui.dp(this, 8);
        box.addView(add, alp);

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(Ui.dp(this, 320), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }
}
