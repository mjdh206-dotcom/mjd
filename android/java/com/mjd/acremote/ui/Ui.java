package com.mjd.acremote.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small UI helpers used to build the interface programmatically. */
public final class Ui {
    public static final int BG = 0xFF05080F;
    public static final int CARD = 0xFF101A2C;
    public static final int CARD_HI = 0xFF17253C;
    public static final int STROKE = 0xFF1E3049;
    public static final int ACCENT = 0xFF35E0C8;
    public static final int BLUE = 0xFF4F8DFF;
    public static final int TEXT = 0xFFF2F7FF;
    public static final int MUTED = 0xFF8CA0BC;

    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable round(int color, float radiusDp, int strokeColor, float strokeDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    public static GradientDrawable gradient(int top, int bottom, float radiusDp, int strokeColor,
                                            float strokeDp, Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{top, bottom});
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView label(Context c, String value) {
        TextView t = text(c, value, 13f, MUTED, false);
        t.setGravity(Gravity.START);
        return t;
    }

    public static TextView button(Context c, String value, boolean active, int accent) {
        TextView t = text(c, value, 14f, active ? Color.parseColor("#04120F") : TEXT, active);
        int padH = dp(c, 16), padV = dp(c, 11);
        t.setPadding(padH, padV, padH, padV);
        t.setBackground(active ? gradient(accent, accent, 18, accent, 0, c)
                : round(CARD, 18, STROKE, 1, c));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static View spacer(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dp)));
        return v;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.weight = w;
        return p;
    }

    public static void ripple(View v) {
        v.setAlpha(0.85f);
        v.animate().alpha(1f).setDuration(120).start();
    }
}
