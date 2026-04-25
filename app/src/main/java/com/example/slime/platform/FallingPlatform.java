package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.slime.GameView;

import java.util.Random;

public class FallingPlatform extends Platform {

    private enum FallState { STABLE, SHAKING, DROPPING }

    private static final int   SHAKE_TICKS  = 90;   // ~1.5 s at 60 fps
    private static final float DROP_GRAVITY = 0.5f;
    private static final float SHAKE_AMP    = 4f;

    private static final Paint TINT = new Paint();
    private static final Rect  SRC  = new Rect(16, 0, 48, 16);

    static {
        TINT.setColor(Color.parseColor("#FF8800"));
        TINT.setAlpha(110);
    }

    private FallState fallState = FallState.STABLE;
    private int       shakeTick = 0;
    private float     dropSpeed = 0f;
    private final Random rng = new Random();

    public FallingPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void update(float screenW) {
        switch (fallState) {
            case SHAKING:
                if (++shakeTick >= SHAKE_TICKS) {
                    fallState = FallState.DROPPING;
                    active = false;
                }
                break;
            case DROPPING:
                dropSpeed += DROP_GRAVITY;
                y += dropSpeed;
                break;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (fallState == FallState.DROPPING) return;

        float drawX = x;
        if (fallState == FallState.SHAKING) {
            drawX += (rng.nextFloat() - 0.5f) * SHAKE_AMP * 2f;
        }
        RectF dst = new RectF(drawX, y, drawX + w, y + 34f);
        if (GameView.platformsBmp != null) {
            canvas.drawBitmap(GameView.platformsBmp, SRC, dst, null);
        }
        // Orange tint warning during shake
        if (fallState == FallState.SHAKING) {
            canvas.drawRoundRect(new RectF(drawX, y, drawX + w, y + h), CORNER, CORNER, TINT);
        }
    }

    @Override
    public float onBounce() {
        if (fallState == FallState.STABLE) {
            fallState = FallState.SHAKING;
            shakeTick = 0;
        }
        return REBOUND_STANDARD;
    }

    @Override
    public boolean canBounce() {
        return fallState == FallState.STABLE;
    }
}
