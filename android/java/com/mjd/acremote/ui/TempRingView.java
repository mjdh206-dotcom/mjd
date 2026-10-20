package com.mjd.acremote.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Circular temperature dial. */
public class TempRingView extends View {
    private static final float START = 135f;
    private static final float SWEEP = 270f;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mode = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private int value = 24;
    private int min = 16, max = 30;
    private String modeText = "تبريد";
    private boolean power = true;

    public TempRingView(Context context) {
        super(context);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        tick.setStyle(Paint.Style.FILL);
        big.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        big.setTextAlign(Paint.Align.CENTER);
        small.setTextAlign(Paint.Align.CENTER);
        small.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mode.setTextAlign(Paint.Align.CENTER);
    }

    public void set(int value, int min, int max, String modeText, boolean power) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.modeText = modeText;
        this.power = power;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float stroke = Ui.dp(getContext(), 16);
        float pad = Ui.dp(getContext(), 30);
        float radius = Math.min(w, h) / 2f - pad;
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        track.setStrokeWidth(stroke);
        track.setColor(0xFF16243A);
        canvas.drawArc(oval, START, SWEEP, false, track);

        float progress = (value - min) / (float) Math.max(1, max - min);
        progress = Math.max(0.02f, Math.min(1f, progress));

        glow.setStrokeWidth(stroke + Ui.dp(getContext(), 12));
        glow.setColor(0x2235E0C8);
        canvas.drawArc(oval, START, SWEEP * progress, false, glow);

        arc.setStrokeWidth(stroke);
        arc.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0xFF4F8DFF, 0xFF35E0C8, 0xFF57F0D0}, null, Shader.TileMode.CLAMP));
        canvas.drawArc(oval, START, SWEEP * progress, false, arc);
        arc.setShader(null);

        tick.setColor(0xFF2A3E5C);
        float tr = radius - stroke - Ui.dp(getContext(), 10);
        for (int i = 0; i <= 12; i++) {
            double a = Math.toRadians(START + SWEEP * i / 12.0);
            float x = cx + tr * (float) Math.cos(a);
            float y = cy + tr * (float) Math.sin(a);
            canvas.drawCircle(x, y, Ui.dp(getContext(), i % 3 == 0 ? 2.2f : 1.4f), tick);
        }

        mode.setColor(power ? Ui.ACCENT : Ui.MUTED);
        mode.setTextSize(Ui.dp(getContext(), 14));
        mode.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        canvas.drawText(power ? modeText : "مغلق", cx, cy - Ui.dp(getContext(), 58), mode);

        big.setColor(power ? Ui.TEXT : 0xFF5A6B84);
        big.setTextSize(Ui.dp(getContext(), 78));
        canvas.drawText(String.valueOf(value), cx, cy + Ui.dp(getContext(), 26), big);

        small.setColor(power ? Ui.ACCENT : 0xFF5A6B84);
        small.setTextSize(Ui.dp(getContext(), 18));
        canvas.drawText("°C", cx, cy + Ui.dp(getContext(), 58), small);
    }
}
