package com.mjd.acremote.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Thin progress bar that fills up while the next code is being prepared. */
public class ScanBarView extends View {
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 0f;

    public ScanBarView(Context context) {
        super(context);
        bg.setColor(0xFF182740);
        fg.setShader(new LinearGradient(0, 0, 600, 0,
                new int[]{0xFF3B82F6, 0xFF35E0C8}, null, Shader.TileMode.CLAMP));
    }

    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = Math.min(getHeight(), Ui.dp(getContext(), 10));
        float r = h / 2f;
        float cy = getHeight() / 2f;
        rect.set(0, cy - r, getWidth(), cy + r);
        canvas.drawRoundRect(rect, r, r, bg);
        if (progress > 0.001f) {
            rect.set(0, cy - r, Math.max(h, getWidth() * progress), cy + r);
            canvas.drawRoundRect(rect, r, r, fg);
        }
    }
}
