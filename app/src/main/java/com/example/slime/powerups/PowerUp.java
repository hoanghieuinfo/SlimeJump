package com.example.slime.powerups;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.slime.entities.PowerUpType;

/**
 * Collectable item that floats above a platform. On contact with the slime it
 * is consumed and applies an effect handled by GameView.
 *
 * Drawn with simple vector shapes so no extra art assets are required.
 */
public class PowerUp {

    public static final float SIZE = 26f;

    private final PowerUpType type;
    private float x, y;
    private boolean collected = false;

    private final Paint bodyPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Gentle vertical bobbing so the icon reads as "pick me up".
    private float bobTick = 0f;

    public PowerUp(PowerUpType type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;

        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(2.5f);
        edgePaint.setColor(Color.WHITE);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextSize(14f);

        switch (type) {
            case JETPACK:    bodyPaint.setColor(Color.parseColor("#FFB020")); break;
            case SHIELD:     bodyPaint.setColor(Color.parseColor("#44AAFF")); break;
            case MULTIPLIER: bodyPaint.setColor(Color.parseColor("#FF4FA3")); break;
        }
    }

    public PowerUpType getType() { return type; }
    public boolean isCollected() { return collected; }
    public void collect() { collected = true; }

    public float getX() { return x; }
    public float getY() { return y; }
    public void scrollDown(float delta) { y += delta; }

    public void update() {
        bobTick += 0.12f;
    }

    public RectF getBounds() {
        float bobY = (float) (Math.sin(bobTick) * 2f);
        return new RectF(x, y + bobY, x + SIZE, y + SIZE + bobY);
    }

    public void draw(Canvas canvas) {
        if (collected) return;
        RectF r = getBounds();
        float cx = r.centerX();
        float cy = r.centerY();

        canvas.drawCircle(cx, cy, SIZE / 2f, bodyPaint);
        canvas.drawCircle(cx, cy, SIZE / 2f, edgePaint);

        String label;
        switch (type) {
            case JETPACK:    label = "R"; break;
            case SHIELD:     label = "S"; break;
            case MULTIPLIER: label = "2x"; break;
            default:         label = "?";
        }
        // Vertically center text on the circle.
        Paint.FontMetrics fm = labelPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, cx, textY, labelPaint);
    }
}
