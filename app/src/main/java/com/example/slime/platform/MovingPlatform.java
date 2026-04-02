package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.slime.GameView;

public class MovingPlatform extends Platform {

    private static final float SPEED = 1.5f; // logical units per frame
    private float direction = 1f;            // +1 = right, -1 = left

    private static final Rect SRC = new Rect(16, 32, 48, 48);

    public MovingPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void update(float screenW) {
        x += SPEED * direction;
        // Reverse at edges (never leave the screen)
        if (x < 0) {
            x = 0;
            direction = 1f;
        } else if (x + w > screenW) {
            x = screenW - w;
            direction = -1f;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (GameView.platformsBmp != null) {
            canvas.drawBitmap(GameView.platformsBmp, SRC, new RectF(x, y, x + w, y + 34f), null);
        }
    }

    @Override
    public float onBounce() {
        return REBOUND_STANDARD;
    }
}
