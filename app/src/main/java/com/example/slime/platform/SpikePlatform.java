package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.slime.GameView;

public class SpikePlatform extends Platform {

    private static final float SPIKE_HEIGHT = 18f;
    private static final int   SPIKE_COUNT  = 5;

    private static final Paint SPIKE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Rect  SRC         = new Rect(16, 0, 48, 16);

    static {
        SPIKE_PAINT.setColor(Color.parseColor("#CC2222"));
    }

    public SpikePlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void draw(Canvas canvas) {
        // Spikes above the base
        Path path = new Path();
        float sw = w / SPIKE_COUNT;
        for (int i = 0; i < SPIKE_COUNT; i++) {
            float sx = x + i * sw;
            path.moveTo(sx, y);
            path.lineTo(sx + sw / 2f, y - SPIKE_HEIGHT);
            path.lineTo(sx + sw, y);
            path.close();
        }
        canvas.drawPath(path, SPIKE_PAINT);

        // Base platform sprite
        if (GameView.platformsBmp != null) {
            canvas.drawBitmap(GameView.platformsBmp, SRC, new RectF(x, y, x + w, y + 34f), null);
        }
    }

    @Override
    public float onBounce() { return 0f; }

    @Override
    public boolean canBounce() { return false; }

    @Override
    public boolean isLethal() { return true; }

    // Collision includes the spike region above the base
    @Override
    public RectF getBounds() {
        return new RectF(x, y - SPIKE_HEIGHT, x + w, y + h);
    }
}
