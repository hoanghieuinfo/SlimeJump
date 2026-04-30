package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.slime.GameView;

// Passes through – faint purple tint is the only visual hint.
public class FakePlatform extends Platform {

    private static final Rect SRC = new Rect(16, 0, 48, 16);

    private static final Paint TINT = new Paint();
    static {
        TINT.setColor(Color.parseColor("#AA3344"));
        TINT.setAlpha(90);
    }

    public FakePlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void draw(Canvas canvas) {
        if (GameView.platformsBmp != null) {
            canvas.drawBitmap(GameView.platformsBmp, SRC, new RectF(x, y, x + w, y + 34f), null);
        }
        canvas.drawRoundRect(new RectF(x, y, x + w, y + h), CORNER, CORNER, TINT);
    }

    @Override
    public float onBounce() { return 0f; }

    @Override
    public boolean canBounce() { return false; }
}
