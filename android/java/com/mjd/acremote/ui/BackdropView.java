package com.mjd.acremote.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Ambient gradient background with soft glows. */
public class BackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BackdropView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF070B14, 0xFF0A1322, 0xFF0C1929},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setShader(new RadialGradient(w * 0.85f, h * 0.08f, w * 0.75f,
                new int[]{0x3335E0C8, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.85f, h * 0.08f, w * 0.75f, paint);

        paint.setShader(new RadialGradient(w * 0.1f, h * 0.85f, w * 0.8f,
                new int[]{0x224F8DFF, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(w * 0.1f, h * 0.85f, w * 0.8f, paint);

        paint.setShader(null);
    }
}
