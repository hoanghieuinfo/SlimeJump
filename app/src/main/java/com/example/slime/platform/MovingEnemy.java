package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class MovingEnemy extends Platform {

    private static final float SPEED = 2.5f;
    private float direction = 1f;

    private static final Paint BODY_PAINT  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint EYE_PAINT   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint PUPIL_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint BROW_PAINT  = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        BODY_PAINT.setColor(Color.parseColor("#E05000"));
        EYE_PAINT.setColor(Color.WHITE);
        PUPIL_PAINT.setColor(Color.parseColor("#1A0000"));
        BROW_PAINT.setColor(Color.parseColor("#1A0000"));
        BROW_PAINT.setStyle(Paint.Style.STROKE);
        BROW_PAINT.setStrokeWidth(2.5f);
        BROW_PAINT.setStrokeCap(Paint.Cap.ROUND);
    }

    public MovingEnemy(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void update(float screenW) {
        x += SPEED * direction;
        if (x < 0)               { x = 0;          direction =  1f; }
        else if (x + w > screenW){ x = screenW - w; direction = -1f; }
    }

    @Override
    public void draw(Canvas canvas) {
        RectF body = new RectF(x, y, x + w, y + h);
        canvas.drawRoundRect(body, h / 2f, h / 2f, BODY_PAINT);

        float eyeR      = h * 0.19f;
        float eyeCY     = y + h * 0.40f;
        float leftEyeX  = x + w * 0.28f;
        float rightEyeX = x + w * 0.72f;
        canvas.drawCircle(leftEyeX,  eyeCY, eyeR, EYE_PAINT);
        canvas.drawCircle(rightEyeX, eyeCY, eyeR, EYE_PAINT);

        float pupilOff = direction * eyeR * 0.35f;
        canvas.drawCircle(leftEyeX  + pupilOff, eyeCY, eyeR * 0.55f, PUPIL_PAINT);
        canvas.drawCircle(rightEyeX + pupilOff, eyeCY, eyeR * 0.55f, PUPIL_PAINT);

        // Angry brows slant inward
        float browY   = eyeCY - eyeR * 1.15f;
        float browHalf = eyeR * 1.3f;
        canvas.drawLine(leftEyeX  - browHalf, browY + eyeR * 0.45f,
                        leftEyeX  + browHalf, browY, BROW_PAINT);
        canvas.drawLine(rightEyeX - browHalf, browY,
                        rightEyeX + browHalf, browY + eyeR * 0.45f, BROW_PAINT);
    }

    @Override
    public float onBounce() { return 0f; }

    @Override
    public boolean canBounce() { return false; }

    @Override
    public boolean isLethal() { return true; }
}
