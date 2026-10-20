package com.mjd.acremote.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Glowing circular power button. */
public class PowerButtonView extends View {
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private boolean on = true;

    public PowerButtonView(Context context) {
        super(context);
        glow.setColor(0x2235E0C8);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        icon.setStyle(Paint.Style.STROKE);
        icon.setStrokeCap(Paint.Cap.ROUND);
        setClickable(true);
        setFocusable(true);
    }

    public void setOn(boolean on) {
        this.on = on;
        invalidate();
    }

    public boolean isOn() {
        return on;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float r = Math.min(w, h) / 2f - Ui.dp(getContext(), 6);

        glow.setColor(on ? 0x3335E0C8 : 0x14FFFFFF);
        canvas.drawCircle(cx, cy, r + Ui.dp(getContext(), 4), glow);

        fill.setColor(on ? 0xFF12353A : 0xFF141C2B);
        canvas.drawCircle(cx, cy, r, fill);

        ring.setStrokeWidth(Ui.dp(getContext(), 3));
        ring.setColor(on ? Ui.ACCENT : 0xFF33445F);
        canvas.drawCircle(cx, cy, r - Ui.dp(getContext(), 2), ring);

        icon.setStrokeWidth(Ui.dp(getContext(), 3.4f));
        icon.setColor(on ? Ui.ACCENT : 0xFF7C8DA8);
        float ir = r * 0.46f;
        rect.set(cx - ir, cy - ir, cx + ir, cy + ir);
        path.reset();
        path.addArc(rect, -60, 300);
        canvas.drawPath(path, icon);
        canvas.drawLine(cx, cy - ir, cx, cy - ir * 0.15f, icon);
    }
}
