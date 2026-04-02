package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.slime.GameView;

public class BouncyPlatform extends Platform {

    private static final Rect SRC = new Rect(16, 16, 48, 32);

    public BouncyPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void draw(Canvas canvas) {
        if (GameView.platformsBmp != null) {
            canvas.drawBitmap(GameView.platformsBmp, SRC, new RectF(x, y, x + w, y + 34f), null);
        }
    }

    @Override
    public float onBounce() {
        return REBOUND_BOUNCY; // Extra-high launch
    }
}
