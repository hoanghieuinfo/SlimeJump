package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.slime.GameView;

public class DisappearingPlatform extends Platform {

    private boolean bounced = false;

    private static final Rect SRC_BEFORE = new Rect(16, 48, 48, 64);
    // afterbreak.png has opaque pixels spanning from y=16 to 32 roughly. 
    // Wait, the safest option to map the full afterbreak sprite cleanly is to map its tight boundary, or entire 0-64 if center mapped.
    // Given the pixel detection earlier, the block starts at Y=16. 
    private static final Rect SRC_AFTER = new Rect(16, 16, 48, 32);

    public DisappearingPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!bounced) {
            if (GameView.platformsBmp != null) {
                canvas.drawBitmap(GameView.platformsBmp, SRC_BEFORE, new RectF(x, y, x + w, y + 34f), null);
            }
        } else {
            if (GameView.afterbreakBmp != null) {
                canvas.drawBitmap(GameView.afterbreakBmp, SRC_AFTER, new RectF(x, y, x + w, y + 34f), null);
            }
        }
    }

    @Override
    public float onBounce() {
        if (!bounced) {
            bounced = true; // Turn cracked – can no longer be bounced
        }
        return REBOUND_STANDARD;
    }

    @Override
    public boolean canBounce() {
        return !bounced; // Only usable before first bounce
    }
}
