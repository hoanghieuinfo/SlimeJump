package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.slime.GameView;
import com.example.slime.Slime;

import java.util.Random;

// Looks like a BouncyPlatform but fires slime sideways instead of straight up.
public class SpringTrapPlatform extends Platform {

    private static final Rect SRC = new Rect(16, 16, 48, 32); // bouncy sprite

    private static final Paint SPRING_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        SPRING_PAINT.setColor(Color.parseColor("#FFD700"));
        SPRING_PAINT.setStyle(Paint.Style.STROKE);
        SPRING_PAINT.setStrokeWidth(3f);
        SPRING_PAINT.setStrokeCap(Paint.Cap.ROUND);
    }

    private final Random rng = new Random();

    public SpringTrapPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void draw(Canvas canvas) {
        if (GameView.platformsBmp != null) {
            canvas.drawBitmap(GameView.platformsBmp, SRC, new RectF(x, y, x + w, y + 34f), null);
        }
        // Zigzag spring coil above centre
        float cx   = x + w / 2f;
        float bot  = y;
        float top  = y - 14f;
        float zw   = 8f;
        float step = (bot - top) / 4f;
        canvas.drawLine(cx,      bot,             cx - zw, bot - step,      SPRING_PAINT);
        canvas.drawLine(cx - zw, bot - step,      cx + zw, bot - step * 2f, SPRING_PAINT);
        canvas.drawLine(cx + zw, bot - step * 2f, cx - zw, bot - step * 3f, SPRING_PAINT);
        canvas.drawLine(cx - zw, bot - step * 3f, cx,      top,             SPRING_PAINT);
    }

    @Override
    public float onBounce() {
        return REBOUND_BOUNCY;
    }

    @Override
    public void applyBounce(Slime slime) {
        slime.dy = REBOUND_BOUNCY;
        // Override sensor steering for ~40 frames so the sideways force is felt
        slime.forceDx     = (rng.nextBoolean() ? 1f : -1f) * (6f + rng.nextFloat() * 7f);
        slime.forceDxTicks = 40;
    }
}
