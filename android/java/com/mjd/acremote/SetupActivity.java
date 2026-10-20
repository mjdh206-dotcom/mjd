package com.mjd.acremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mjd.acremote.ac.AcState;
import com.mjd.acremote.ac.Protocol;
import com.mjd.acremote.ac.Protocols;
import com.mjd.acremote.data.Profile;
import com.mjd.acremote.data.Store;
import com.mjd.acremote.ir.IrBlaster;
import com.mjd.acremote.ir.Signal;
import com.mjd.acremote.ui.BackdropView;
import com.mjd.acremote.ui.ScanBarView;
import com.mjd.acremote.ui.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Onboarding + smart scanning wizard. */
public class SetupActivity extends Activity {
    private static final int STEP_WELCOME = 0;
    private static final int STEP_BRAND = 1;
    private static final int STEP_TEST = 2;

    private Store store;
    private IrBlaster blaster;
    private FrameLayout root;
    private LinearLayout content;
    private int step = STEP_WELCOME;

    private static final long AUTO_GAP_MS = 1400;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScanBarView scanBar;
    private TextView scanStatus;
    private TextView scanCount;
    private TextView autoButton;
    private TextView scanInfo;
    private boolean autoRunning = false;
    private boolean found = false;
    private int attemptCount = 0;
    private int scanPass = 0;

    // scan state
    private boolean smartScan = true;
    private Protocol brandProtocol;
    private List<Protocol> scanList = new ArrayList<>();
    private List<Integer> scanVariants = new ArrayList<>();
    private int scanIndex = 0;
    private boolean awaitingAnswer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        store = new Store(this);
        blaster = new IrBlaster(this);
        buildShell();
        showStep(STEP_WELCOME);
    }

    private void buildShell() {
        root = new FrameLayout(this);
        root.addView(new BackdropView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 28));
        scroll.addView(content);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private void showStep(int newStep) {
        step = newStep;
        content.removeAllViews();
        switch (step) {
            case STEP_WELCOME:
                buildWelcome();
                break;
            case STEP_BRAND:
                buildBrands();
                break;
            default:
                buildTest();
                break;
        }
    }

    // ----------------------------------------------------------- welcome ---

    private void buildWelcome() {
        content.addView(brandMark());
        content.addView(gap(22));

        TextView title = Ui.text(this, "ريموت المكيف برو", 26f, Ui.TEXT, true);
        title.setGravity(Gravity.START);
        content.addView(title);

        TextView sub = Ui.text(this, "تحكّم كامل في مكيفك من هاتفك، بدون إنترنت وبدون إعدادات معقّدة.",
                13f, Ui.MUTED, false);
        sub.setGravity(Gravity.START);
        sub.setLineSpacing(Ui.dp(this, 4), 1f);
        sub.setPadding(0, Ui.dp(this, 8), 0, 0);
        content.addView(sub);

        content.addView(gap(24));

        LinearLayout card = card();
        LinearLayout head = Ui.row(this);
        TextView dot = Ui.text(this, blaster.hasEmitter() ? "●" : "●", 14f,
                blaster.hasEmitter() ? Ui.ACCENT : 0xFFFF7272, true);
        TextView st = Ui.text(this, blaster.hasEmitter() ? "هاتفك يدعم الأشعة تحت الحمراء"
                : "لم نجد باعث أشعة تحت الحمراء", 14f, Ui.TEXT, true);
        st.setGravity(Gravity.START);
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        stlp.setMarginStart(Ui.dp(this, 8));
        head.addView(dot);
        head.addView(st, stlp);
        card.addView(head);

        TextView desc = Ui.text(this, blaster.hasEmitter()
                ? "الحساسية: " + blaster.describeHardware()
                : "هذه الميزة تحتاج هاتفًا بباعث IR (مثل Xiaomi، Huawei، LG، HTC). التطبيق سيعمل لكن الإرسال لن يصل للمكيف.",
                11.5f, Ui.MUTED, false);
        desc.setGravity(Gravity.START);
        desc.setPadding(0, Ui.dp(this, 8), 0, 0);
        card.addView(desc);
        content.addView(card);

        content.addView(gap(18));
        content.addView(featureRow("🎯", "كشف ذكي", "يتعرّف على نوع المكيف تلقائيًا عبر إرسال أكواد اختبار."));
        content.addView(gap(10));
        content.addView(featureRow("🌡", "تحكم كامل", "تبريد، تدفئة، تجفيف، مروحة، تأرجح، تيربو ومؤقت إطفاء."));
        content.addView(gap(10));
        content.addView(featureRow("💾", "أجهزة متعددة", "احفظ أكثر من مكيف وبدّل بينها بضغطة واحدة."));

        content.addView(gap(26));
        TextView start = primaryButton("ابدأ الإعداد  ←");
        start.setOnClickListener(v -> showStep(STEP_BRAND));
        content.addView(start);
    }

    private View brandMark() {
        LinearLayout box = Ui.row(this);
        TextView logo = Ui.text(this, "❄", 26f, 0xFF04120F, true);
        logo.setBackground(Ui.gradient(Ui.ACCENT, Ui.BLUE, 20, Ui.ACCENT, 0, this));
        box.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 58)));
        TextView name = Ui.text(this, "PRO", 12f, Ui.ACCENT, true);
        name.setLetterSpacing(0.3f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Ui.dp(this, 12));
        box.addView(name, lp);
        return box;
    }

    private View featureRow(String icon, String title, String desc) {
        LinearLayout card = card();
        LinearLayout row = Ui.row(this);
        TextView ic = Ui.text(this, icon, 18f, Ui.ACCENT, false);
        ic.setBackground(Ui.round(0x1A35E0C8, 14, 0, 0, this));
        ic.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        row.addView(ic);
        LinearLayout texts = Ui.column(this);
        TextView t = Ui.text(this, title, 13.5f, Ui.TEXT, true);
        t.setGravity(Gravity.START);
        TextView d = Ui.text(this, desc, 11f, Ui.MUTED, false);
        d.setGravity(Gravity.START);
        d.setPadding(0, Ui.dp(this, 3), 0, 0);
        texts.addView(t);
        texts.addView(d);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMarginStart(Ui.dp(this, 12));
        row.addView(texts, tlp);
        card.addView(row);
        return card;
    }

    // ----------------------------------------------------------- brands ----

    private void buildBrands() {
        content.addView(backRow("اختيار العلامة التجارية"));
        content.addView(gap(16));

        LinearLayout smart = Ui.row(this);
        smart.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        smart.setBackground(Ui.gradient(0xFF123A3C, 0xFF102B36, 22, Ui.ACCENT, 1, this));
        LinearLayout texts = Ui.column(this);
        TextView t = Ui.text(this, "⚡  بحث ذكي (موصى به)", 14.5f, Ui.TEXT, true);
        t.setGravity(Gravity.START);
        TextView d = Ui.text(this, "جرّب الأكواد تلقائيًا حتى يستجيب مكيفك", 11f, 0xFFA9D8D2, false);
        d.setGravity(Gravity.START);
        d.setPadding(0, Ui.dp(this, 4), 0, 0);
        texts.addView(t);
        texts.addView(d);
        smart.addView(texts, Ui.weight(1f));
        TextView arrow = Ui.text(this, "←", 18f, Ui.ACCENT, true);
        smart.addView(arrow);
        smart.setOnClickListener(v -> {
            smartScan = true;
            brandProtocol = null;
            startScan();
        });
        content.addView(smart);

        content.addView(gap(22));
        TextView or = Ui.text(this, "أو اختر علامة المكيف يدويًا", 12f, Ui.MUTED, false);
        content.addView(or);
        content.addView(gap(12));

        LinearLayout grid = Ui.column(this);
        List<Protocol> all = Protocols.all();
        for (int i = 0; i < all.size(); i += 2) {
            LinearLayout row = Ui.row(this);
            row.addView(brandTile(all.get(i)), Ui.weight(1f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginStart(Ui.dp(this, 10));
            if (i + 1 < all.size()) {
                row.addView(brandTile(all.get(i + 1)), lp);
            } else {
                View fill = new View(this);
                row.addView(fill, lp);
            }
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = Ui.dp(this, 10);
            grid.addView(row, rlp);
        }
        content.addView(grid);
    }

    private View brandTile(final Protocol p) {
        LinearLayout box = Ui.column(this);
        box.setPadding(Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16));
        box.setBackground(Ui.round(Ui.CARD, 18, Ui.STROKE, 1, this));
        TextView n = Ui.text(this, p.nameAr, 14f, Ui.TEXT, true);
        n.setGravity(Gravity.START);
        TextView e = Ui.text(this, p.nameEn, 10f, Ui.MUTED, false);
        e.setGravity(Gravity.START);
        e.setPadding(0, Ui.dp(this, 3), 0, 0);
        TextView range = Ui.text(this, p.minTemp + "° – " + p.maxTemp + "°", 10f, 0xFF4C688C, false);
        range.setGravity(Gravity.START);
        range.setPadding(0, Ui.dp(this, 8), 0, 0);
        box.addView(n);
        box.addView(e);
        box.addView(range);
        box.setOnClickListener(v -> {
            smartScan = false;
            brandProtocol = p;
            startScan();
        });
        return box;
    }

    // ------------------------------------------------------------- test ----

    private void startScan() {
        stopAuto();
        found = false;
        attemptCount = 0;
        irWarned = false;
        scanList.clear();
        scanVariants.clear();
        scanIndex = 0;
        if (smartScan) {
            for (Protocol p : Protocols.scanOrder()) {
                for (int v = 0; v < p.variantCount(); v++) {
                    scanList.add(p);
                    scanVariants.add(v);
                }
            }
        } else {
            for (int v = 0; v < brandProtocol.variantCount(); v++) {
                scanList.add(brandProtocol);
                scanVariants.add(v);
            }
        }
        scanPass = 0;
        showStep(STEP_TEST);
    }

    private void buildTest() {
        Protocol current = scanList.get(scanIndex);
        int total = scanList.size();

        content.addView(backRow(brandProtocol == null ? "البحث التلقائي" : "تجربة " + brandProtocol.nameAr));
        content.addView(gap(10));

        LinearLayout card = card();
        card.setGravity(Gravity.CENTER);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 22), Ui.dp(this, 16), Ui.dp(this, 20));

        TextView icon = Ui.text(this, "📡", 36f, Ui.TEXT, false);
        card.addView(icon);
        card.addView(gap(8));

        TextView title = Ui.text(this, "البحث التلقائي يعمل الآن", 16f, Ui.TEXT, true);
        card.addView(title);
        card.addView(gap(6));

        TextView hint = Ui.text(this,
                "وجّه الهاتف نحو المكيف واتركه.\nسيرسل التطبيق الأكواد واحدة تلو الأخرى تلقائيًا.\nبمجرد صدور صوت من المكيف أو تغيّر حالته اضغط الزر الكبير.",
                12f, Ui.MUTED, false);
        hint.setLineSpacing(Ui.dp(this, 4), 1f);
        card.addView(hint);
        content.addView(card);

        content.addView(gap(18));
        scanBar = new ScanBarView(this);
        content.addView(scanBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 10)));
        content.addView(gap(8));

        LinearLayout info = Ui.row(this);
        scanStatus = Ui.text(this, "جارٍ التجربة…", 12f, Ui.TEXT, true);
        scanStatus.setGravity(Gravity.START);
        scanCount = Ui.text(this, "", 11f, Ui.MUTED, false);
        info.addView(scanStatus, Ui.weight(1f));
        info.addView(scanCount);
        content.addView(info);

        content.addView(gap(18));

        // big "found it" button
        TextView foundButton = Ui.text(this, "🛑  وجدته! أوقف البحث", 17f, 0xFF04120F, true);
        foundButton.setBackground(Ui.gradient(Ui.ACCENT, 0xFF3BC8E8, 22, Ui.ACCENT, 0, this));
        foundButton.setPadding(0, Ui.dp(this, 20), 0, Ui.dp(this, 20));
        foundButton.setElevation(Ui.dp(this, 6));
        foundButton.setOnClickListener(v -> {
            if (found) return;
            onSuccess();
        });
        content.addView(foundButton);

        content.addView(gap(12));

        autoButton = Ui.text(this, autoRunning ? "⏸  إيقاف مؤقت" : "▶  متابعة البحث التلقائي",
                13f, Ui.TEXT, false);
        autoButton.setBackground(Ui.round(Ui.CARD, 18, Ui.STROKE, 1, this));
        autoButton.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        autoButton.setOnClickListener(v -> toggleAuto());
        content.addView(autoButton);

        content.addView(gap(10));

        LinearLayout answers = Ui.row(this);
        TextView no = Ui.text(this, "✕  لم يستجب — التالي", 13f, 0xFFFFB4B4, true);
        no.setBackground(Ui.round(0xFF2A1720, 18, 0xFF5A2733, 1, this));
        no.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        no.setOnClickListener(v -> {
            if (found) return;
            onNext();
        });
        answers.addView(no, Ui.weight(1f));
        content.addView(answers);

        content.addView(gap(14));
        scanInfo = Ui.text(this, "", 11.5f, 0xFF4C688C, false);
        scanInfo.setLineSpacing(Ui.dp(this, 3), 1f);
        content.addView(scanInfo);

        updateScanLabels();
        if (!autoRunning) startAuto();
    }

    private Protocol currentProtocol() {
        if (scanList.isEmpty()) return Protocols.all().get(0);
        return scanList.get(Math.min(scanIndex, scanList.size() - 1));
    }

    private void updateScanLabels() {
        if (scanStatus == null || scanCount == null) return;
        int total = scanList.size();
        scanStatus.setText(found ? "تم إيجاد الكود ✓" :
                (autoRunning ? "جارٍ الإرسال التلقائي…" : "متوقف مؤقتًا"));
        scanCount.setText((scanIndex + 1) + " / " + total + " · " + attemptCount + " إشارة");
        if (scanBar != null) {
            scanBar.setProgress(total == 0 ? 0 : (scanIndex + 1f) / total);
        }
        if (autoButton != null) {
            autoButton.setText(autoRunning ? "⏸  إيقاف مؤقت" : "▶  متابعة البحث التلقائي");
        }
        if (scanInfo != null && !scanList.isEmpty()) {
            Protocol p = scanList.get(Math.min(scanIndex, scanList.size() - 1));
            String text = "الكود الحالي: " + p.nameAr + " (" + p.nameEn + ")"
                    + " — " + (scanIndex + 1) + " من " + total
                    + "\nالجولة " + (scanPass + 1)
                    + (autoRunning ? " · البحث مستمر تلقائيًا" : " · البحث متوقف");
            scanInfo.setText(text);
        }
    }

    private void toggleAuto() {
        if (autoRunning) {
            stopAuto();
        } else {
            startAuto();
        }
        updateScanLabels();
    }

    private void startAuto() {
        stopAuto();
        if (!blaster.hasEmitter() && !irWarned) {
            irWarned = true;
            hintNoIr();
        }
        autoRunning = true;
        handler.post(autoStep);
    }

    private void stopAuto() {
        autoRunning = false;
        handler.removeCallbacks(autoStep);
        handler.removeCallbacks(autoNext);
    }

    /** Sends the code that is currently selected, then schedules the next one. */
    private final Runnable autoStep = new Runnable() {
        @Override
        public void run() {
            if (!autoRunning || found) return;
            if (scanList.isEmpty() || scanIndex >= scanList.size()) {
                rebuildForNextPass();
                if (scanList.isEmpty()) return;
            }
            Protocol p = scanList.get(scanIndex);
            int variant = scanVariants.get(scanIndex);
            p.variant = variant;
            Signal signal = p.variantCount() > 1 ? p.testSignal(variant) : p.testSignal();
            attemptCount++;
            boolean ok = blaster.transmit(signal);
            if (!ok && !irWarned) {
                irWarned = true;
                hintNoIr();
            }
            updateScanLabels();
            handler.postDelayed(autoNext, AUTO_GAP_MS);
        }
    };

    /** Moves on to the following code of the sweep. */
    private final Runnable autoNext = new Runnable() {
        @Override
        public void run() {
            if (!autoRunning || found) return;
            if (scanIndex + 1 < scanList.size()) {
                scanIndex++;
            } else if (smartScan) {
                rebuildForNextPass();
            } else {
                scanIndex = 0;
            }
            handler.post(autoStep);
        }
    };

    /**
     * When every code of the current pass has been tried, start the next pass
     * (alternate protocol variants), then keep cycling until the user stops it.
     */
    private void rebuildForNextPass() {
        scanPass++;
        List<Protocol> list = new ArrayList<>();
        List<Integer> variants = new ArrayList<>();
        if (smartScan) {
            for (Protocol p : Protocols.scanOrder()) {
                if (p.variantCount() > scanPass % Math.max(1, maxVariants())) {
                    list.add(p);
                    variants.add(scanPass % maxVariants());
                }
            }
            if (list.isEmpty()) {
                for (Protocol p : Protocols.scanOrder()) {
                    list.add(p);
                    variants.add(0);
                }
            }
        } else if (brandProtocol != null) {
            int v = scanPass % Math.max(1, brandProtocol.variantCount());
            list.add(brandProtocol);
            variants.add(v);
        }
        scanList.clear();
        scanVariants.clear();
        scanList.addAll(list);
        scanVariants.addAll(variants);
        scanIndex = 0;
    }

    private int maxVariants() {
        int max = 1;
        for (Protocol p : Protocols.all()) max = Math.max(max, p.variantCount());
        return max;
    }

    private boolean irWarned = false;

    private void sendTest() {
        if (scanList.isEmpty()) return;
        Protocol p = scanList.get(scanIndex);
        int variant = scanVariants.get(scanIndex);
        p.variant = variant;
        Signal s = p.variantCount() > 1 ? p.testSignal(variant) : p.testSignal();
        boolean ok = blaster.transmit(s);
        if (!ok && !irWarned) {
            irWarned = true;
            hintNoIr();
        }
        attemptCount++;
        updateScanLabels();
    }

    private void hintNoIr() {
        new AlertDialog.Builder(this)
                .setTitle("لا يوجد باعث IR")
                .setMessage("هذا الهاتف لا يحتوي على باعث أشعة تحت الحمراء، لذلك لن تصل الإشارة إلى المكيف.")
                .setPositiveButton("حسنًا", null)
                .show();
    }

    private void onNext() {
        if (scanList.isEmpty()) return;
        if (scanIndex + 1 >= scanList.size()) {
            new AlertDialog.Builder(this)
                    .setTitle("لم يتم العثور على كود مطابق")
                    .setMessage("جرّب البحث مرة أخرى مع توجيه الهاتف مباشرة نحو المكيف من مسافة قريبة (أقل من متر).")
                    .setPositiveButton("إعادة المحاولة", (d, w) -> startScan())
                    .setNegativeButton("رجوع", (d, w) -> showStep(STEP_BRAND))
                    .show();
            return;
        }
        if (scanIndex + 1 < scanList.size()) scanIndex++;
        else rebuildForNextPass();
        if (autoRunning) {
            handler.removeCallbacks(autoStep);
            handler.removeCallbacks(autoNext);
            handler.postDelayed(autoStep, 320);
        } else {
            sendTest();
        }
        updateScanLabels();
    }

    private void onSuccess() {
        found = true;
        stopAuto();
        final Protocol p = currentProtocol();
        final int variant = scanVariants.isEmpty() ? 0
                : scanVariants.get(Math.min(scanIndex, scanVariants.size() - 1));
        p.variant = variant;
        // Re-send so the unit is in a known state.
        blaster.transmit(p.encode(p.defaultState()));
        final EditText input = new EditText(this);
        input.setHint("مثال: مكيف الصالة");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xFF6E86A6);
        input.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 12));
        input.setBackground(Ui.round(0xFF16243A, 14, Ui.STROKE, 1, this));
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setPadding(Ui.dp(this, 18), Ui.dp(this, 6), Ui.dp(this, 18), 0);
        wrapper.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("تم العثور على المكيف 🎉")
                .setMessage("تم التعرف عليه: " + p.nameAr + "\nأدخل اسمًا للمكيف:")
                .setView(wrapper)
                .setPositiveButton("حفظ", null)
                .setNegativeButton("إلغاء", null)
                .create();
        dialog.setOnShowListener(d -> {
            TextView positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setTextColor(Ui.ACCENT);
            positive.setOnClickListener(v -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) name = "مكيف " + p.nameAr;
                save(p, name, variant);
                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Ui.MUTED);
        });
        dialog.setOnDismissListener(d -> finish());
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) w.setBackgroundDrawable(new ColorDrawable(0xFF101B2E));
    }

    private void save(Protocol p, String name, int variant) {
        Profile profile = new Profile(UUID.randomUUID().toString(), name, p.id);
        profile.variant = variant;
        profile.mode = AcState.MODE_COOL;
        profile.temp = 24;
        profile.fan = AcState.FAN_AUTO;
        profile.power = true;
        store.upsert(profile);
        store.setOnboarded();
        startActivity(new android.content.Intent(this, MainActivity.class));
        finish();
    }

    // ---------------------------------------------------------- helpers ----

    private LinearLayout card() {
        LinearLayout card = Ui.column(this);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        card.setBackground(Ui.gradient(0xFF111D31, 0xFF0D1727, 22, Ui.STROKE, 1, this));
        return card;
    }

    private TextView primaryButton(String label) {
        TextView b = Ui.text(this, label, 15f, 0xFF04120F, true);
        b.setBackground(Ui.gradient(Ui.ACCENT, 0xFF3BC8E8, 20, Ui.ACCENT, 0, this));
        b.setPadding(0, Ui.dp(this, 17), 0, Ui.dp(this, 17));
        b.setClickable(true);
        b.setElevation(Ui.dp(this, 6));
        return b;
    }

    private View backRow(String title) {
        LinearLayout row = Ui.row(this);
        TextView back = Ui.text(this, "→", 20f, Ui.TEXT, false);
        back.setBackground(Ui.round(Ui.CARD, 14, Ui.STROKE, 1, this));
        back.setOnClickListener(v -> {
            if (step == STEP_TEST) showStep(STEP_BRAND);
            else if (step == STEP_BRAND) showStep(STEP_WELCOME);
        });
        row.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        TextView t = Ui.text(this, title, 16f, Ui.TEXT, true);
        t.setGravity(Gravity.START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(Ui.dp(this, 12));
        row.addView(t, lp);
        return row;
    }

    private View gap(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, Ui.dp(this, dp)));
        return v;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAuto();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAuto();
    }

    @Override
    public void onBackPressed() {
        if (step == STEP_TEST) {
            stopAuto();
            showStep(STEP_BRAND);
            return;
        }
        if (step == STEP_BRAND && store.active() != null) {
            finish();
            return;
        }
        if (step == STEP_BRAND) return;
        super.onBackPressed();
    }
}
