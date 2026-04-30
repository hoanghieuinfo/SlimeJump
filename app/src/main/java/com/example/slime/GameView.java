package com.example.slime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Rect;

import com.example.slime.entities.BackgroundTheme;
import com.example.slime.entities.SlimeState;
import com.example.slime.platform.BouncyPlatform;
import com.example.slime.platform.DisappearingPlatform;
import com.example.slime.platform.FakePlatform;
import com.example.slime.platform.FallingPlatform;
import com.example.slime.platform.MovingEnemy;
import com.example.slime.platform.MovingPlatform;
import com.example.slime.platform.Platform;
import com.example.slime.platform.SpikePlatform;
import com.example.slime.platform.SpringTrapPlatform;
import com.example.slime.platform.StandardPlatform;

import com.example.slime.entities.PowerUpType;
import com.example.slime.powerups.PowerUp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView
        implements SurfaceHolder.Callback, SensorEventListener {

    private static final float GW = 400f;
    private static final float GH = 700f;

    private static final float GRAVITY    = 0.20f;
    private static final float INITIAL_DY = -10f;
    private static final float PW = 68f;
    private static final float PH = 14f;
    private static final float SPACING = 80f;
    private static final int   PLAT_COUNT = 10;
    private static final float WRAP_LEFT  = -50f;
    private static final float WRAP_RIGHT = GW + 50f;
    private static final float SENSOR_SPEED = 0.8f;

    private static final String PREFS = "slime_prefs";
    private static final String KEY_STEPS_ACCUMULATED = "steps_accumulated";
    private static final int STEPS_PER_SHIELD = 50;

    private enum State { PLAYING, GAME_OVER }
    private State gameState = State.PLAYING;

    private Slime slime;
    private final List<Platform> platforms = new ArrayList<>();
    private final Random rng = new Random();

    private final List<PowerUp> powerUps = new ArrayList<>();
    private int scoreMultiplier = 1;
    private int multiplierTicks = 0;
    private static final int MULTIPLIER_DURATION = 300;
    private static final float JETPACK_BOOST = -22f;

    private int score = 0;
    private float sensorX = 0f;
    private SensorManager sensorManager;
    private float scaleX = 1f, scaleY = 1f;
    private float density = 1f;

    private BackgroundTheme currentTheme;
    private Bitmap bgImg;

    private SpriteSheet spriteSheet;
    public static Bitmap platformsBmp;
    public static Bitmap afterbreakBmp;
    private Paint scorePaint;
    private Paint shieldPaint;
    private Paint shieldBgPaint;
    private Paint pauseBtnBgPaint;
    private Paint pauseIconPaint;
    private Paint overlayPaint;
    private Paint pauseLabelPaint;
    private RectF pauseBtnRect;
    private volatile boolean userPaused = false;

    private GameThread gameThread;

    private boolean hasShield;
    private boolean shieldActive;

    public interface GameOverListener {
        void onGameOver(int score);
    }
    private GameOverListener gameOverListener;

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    public GameView(Context context, BackgroundTheme theme, boolean hasShield) {
        super(context);
        this.currentTheme = theme;
        this.hasShield = hasShield;
        this.shieldActive = hasShield;
        density = context.getResources().getDisplayMetrics().density;
        getHolder().addCallback(this);
        setFocusable(true);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        initPaints();
    }

    private void initPaints() {
        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(18f * density);
        try {
            Typeface pixelFont = ResourcesCompat.getFont(getContext(), R.font.dogicapixel);
            scorePaint.setTypeface(pixelFont);
        } catch (Exception e) {
            scorePaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
        scorePaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#44000000"));

        shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldPaint.setColor(Color.WHITE);
        shieldPaint.setTextSize(14f * density);
        shieldPaint.setTypeface(scorePaint.getTypeface());
        shieldPaint.setShadowLayer(4f, 1f, 1f, Color.parseColor("#88000000"));

        shieldBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldBgPaint.setColor(Color.parseColor("#8800AABB"));
        shieldBgPaint.setStyle(Paint.Style.FILL);

        pauseBtnBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pauseBtnBgPaint.setColor(Color.parseColor("#E61a1a2e"));
        pauseBtnBgPaint.setStyle(Paint.Style.FILL);
        pauseBtnBgPaint.setShadowLayer(8f, 0f, 4f, Color.parseColor("#88000000"));

        pauseIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pauseIconPaint.setColor(Color.WHITE);
        pauseIconPaint.setTextSize(30f * density);
        pauseIconPaint.setTextAlign(Paint.Align.CENTER);
        pauseIconPaint.setTypeface(Typeface.DEFAULT_BOLD);
        pauseIconPaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#88000000"));

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#CC000000"));

        pauseLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pauseLabelPaint.setColor(Color.WHITE);
        pauseLabelPaint.setTextSize(36f * density);
        pauseLabelPaint.setTextAlign(Paint.Align.CENTER);
        pauseLabelPaint.setShadowLayer(6f, 3f, 3f, Color.parseColor("#88000000"));
        try {
            pauseLabelPaint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.dogicapixel));
        } catch (Exception e) {
            pauseLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Bitmap raw = BitmapFactory.decodeResource(getResources(), R.drawable.slimejump);
        spriteSheet = new SpriteSheet(raw);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;

        platformsBmp  = BitmapFactory.decodeResource(getResources(), R.drawable.platforms, opts);
        afterbreakBmp = BitmapFactory.decodeResource(getResources(), R.drawable.afterbreak, opts);

        int bgResId = (currentTheme == BackgroundTheme.DAY) ? R.drawable.cloudsday : R.drawable.cloudsnight;
        bgImg = BitmapFactory.decodeResource(getResources(), bgResId, opts);

        startGame();

        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }

        gameThread = new GameThread(holder, this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    private Rect srcBgRect = new Rect();
    private RectF dstBgRect = new RectF();

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        scaleX = w / GW;
        scaleY = h / GH;

        if (bgImg != null) {
            float imgRatio    = (float) bgImg.getWidth() / bgImg.getHeight();
            float screenRatio = (float) w / h;

            int srcX = 0, srcY = 0, srcW = bgImg.getWidth(), srcH = bgImg.getHeight();

            if (screenRatio > imgRatio) {
                srcH = (int) (srcW / screenRatio);
                srcY = (bgImg.getHeight() - srcH) / 2;
            } else {
                srcW = (int) (srcH * screenRatio);
                srcX = (bgImg.getWidth() - srcW) / 2;
            }

            srcBgRect.set(srcX, srcY, srcX + srcW, srcY + srcH);
            dstBgRect.set(0, 0, w, h);
        }

        float btnSize = 72f * density;
        float btnMargin = 14f * density;
        pauseBtnRect = new RectF(w - btnSize - btnMargin, btnMargin, w - btnMargin, btnMargin + btnSize);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        sensorManager.unregisterListener(this);
        if (gameThread != null) {
            gameThread.setRunning(false);
            try { gameThread.join(1000); } catch (InterruptedException ignored) {}
        }
    }

    private void startGame() {
        platforms.clear();
        powerUps.clear();
        score = 0;
        scoreMultiplier = 1;
        multiplierTicks = 0;
        userPaused = false;
        gameState = State.PLAYING;

        float cx    = GW / 2f - PW / 2f;
        float firstY = GH - 60f;
        platforms.add(new StandardPlatform(cx, firstY, PW, PH));

        float topY = firstY - SPACING;
        for (int i = 1; i < PLAT_COUNT; i++) {
            spawnPlatform(topY);
            topY -= SPACING;
        }

        slime = new Slime(spriteSheet, GW / 2f, firstY - Slime.SIZE);
        slime.dy = INITIAL_DY;
        slime.setState(SlimeState.LAUNCH);
    }

    void update() {
        if (!userPaused && gameState == State.PLAYING) {
            updatePlaying();
        }
    }

    private void updatePlaying() {
        // SpringTrap temporarily overrides sensor-driven steering
        if (slime.forceDxTicks > 0) {
            slime.dx = slime.forceDx;
            slime.forceDxTicks--;
        } else {
            slime.dx = -sensorX * SENSOR_SPEED;
        }
        slime.updateFacing();
        slime.x += slime.dx;
        wrapSlime();

        slime.dy += GRAVITY;

        float newY = slime.y + slime.dy;
        float midY = GH / 2f;

        if (newY < midY) {
            float excess = midY - newY;
            slime.y = midY;
            Platform lowest = null;
            for (Platform p : platforms) {
                p.scrollDown(excess);
                if (lowest == null || p.getY() > lowest.getY()) lowest = p;
            }
            for (PowerUp pu : powerUps) pu.scrollDown(excess);
            if (slime.dy <= 0) {
                score += (int) (excess / 5f) * scoreMultiplier;
            }
        } else {
            slime.y = newY;
        }

        if (slime.isFalling() && slime.getState() == SlimeState.FALLING) {
            for (Platform p : platforms) {
                if (p.canBounce() && slimeLandsOn(p)) {
                    p.applyBounce(slime);
                    slime.setState(SlimeState.LANDING);
                    break;
                }
            }
        }

        // Lethal hazard check (SpikePlatform, MovingEnemy)
        for (Platform p : platforms) {
            if (p.isLethal() && slimeTouches(p)) {
                if (shieldActive) {
                    activateShieldRescue();
                } else {
                    gameOver();
                }
                return;
            }
        }

        slime.updateAnimation();

        if (multiplierTicks > 0 && --multiplierTicks == 0) scoreMultiplier = 1;

        Iterator<PowerUp> puIter = powerUps.iterator();
        while (puIter.hasNext()) {
            PowerUp pu = puIter.next();
            pu.update();
            if (pu.getY() > GH + 50f) { puIter.remove(); continue; }
            if (!pu.isCollected()) {
                RectF pb = pu.getBounds();
                if (pb.right > slime.x && pb.left < slime.x + Slime.SIZE
                        && pb.bottom > slime.y && pb.top < slime.y + Slime.SIZE) {
                    pu.collect();
                    applyPowerUp(pu.getType());
                }
            } else {
                puIter.remove();
            }
        }

        for (Platform p : platforms) {
            p.update(GW);
        }

        recycleAndGenerate();

        if (slime.y > GH) {
            if (shieldActive) {
                activateShieldRescue();
            } else {
                gameOver();
            }
        }
    }

    private void applyPowerUp(PowerUpType type) {
        switch (type) {
            case JETPACK:
                slime.dy = JETPACK_BOOST;
                slime.setState(SlimeState.LAUNCH);
                break;
            case SHIELD:
                shieldActive = true;
                break;
            case MULTIPLIER:
                scoreMultiplier = 2;
                multiplierTicks = MULTIPLIER_DURATION;
                break;
        }
    }

    private void activateShieldRescue() {
        shieldActive = false;
        consumeShieldSteps();
        slime.y = GH / 2f;
        slime.dy = INITIAL_DY;
        slime.setState(SlimeState.LAUNCH);
    }

    private void consumeShieldSteps() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long accumulated = prefs.getLong(KEY_STEPS_ACCUMULATED, 0);
        long newAccumulated = Math.max(0, accumulated - STEPS_PER_SHIELD);
        prefs.edit().putLong(KEY_STEPS_ACCUMULATED, newAccumulated).apply();
    }

    // Full bounding-box overlap used for lethal hazards (spikes, enemies)
    private boolean slimeTouches(Platform p) {
        final float margin = 8f;
        float sl = slime.x + margin;
        float sr = slime.x + Slime.SIZE - margin;
        float st = slime.y + margin;
        float sb = slime.y + Slime.SIZE - margin;
        RectF pb = p.getBounds();
        return sr > pb.left && sl < pb.right && sb > pb.top && st < pb.bottom;
    }

    private boolean slimeLandsOn(Platform p) {
        float sl = slime.x + 10f;
        float sr = slime.x + Slime.SIZE - 10f;
        float sb = slime.y + Slime.SIZE;
        float pt = p.getY();
        float pl = p.getX();
        float pr = p.getX() + p.getW();

        return sb >= pt && sb <= pt + p.getH() + Math.abs(slime.dy) + 2f
                && sr > pl && sl < pr;
    }

    private void wrapSlime() {
        if (slime.x + Slime.SIZE < WRAP_LEFT)  slime.x = WRAP_RIGHT - Slime.SIZE;
        else if (slime.x > WRAP_RIGHT)          slime.x = WRAP_LEFT;
    }

    private void recycleAndGenerate() {
        float highestY = GH;
        for (Platform p : platforms) {
            if (p.getY() < highestY) highestY = p.getY();
        }

        List<Platform> toRemove = new ArrayList<>();
        for (Platform p : platforms) {
            if (p.getY() > GH + 20f) toRemove.add(p);
        }

        platforms.removeAll(toRemove);
        for (int i = 0; i < toRemove.size(); i++) {
            highestY -= SPACING;
            spawnPlatform(highestY);
        }
    }

    private static final float ENEMY_W = 52f;
    private static final float ENEMY_H = 28f;

    private void spawnPlatform(float y) {
        int type = rng.nextInt(100);
        Platform p;
        // 0-44  Standard(45%) 45-54 Disappearing(10%) 55-64 Bouncy(10%)
        // 65-74 Moving(10%)   75-82 Falling(8%)       83-88 Fake(6%)
        // 89-93 Spike(5%)     94-97 MovingEnemy(4%)   98-99 SpringTrap(2%)
        if (type < 45) {
            float px = rng.nextFloat() * (GW - PW);
            p = new StandardPlatform(px, y, PW, PH);
            if (rng.nextInt(100) < 15) {
                PowerUpType puType = PowerUpType.values()[rng.nextInt(PowerUpType.values().length)];
                float puX = px + (PW - PowerUp.SIZE) / 2f;
                powerUps.add(new PowerUp(puType, puX, y - PowerUp.SIZE - 8f));
            }
        } else if (type < 55) {
            p = new DisappearingPlatform(rng.nextFloat() * (GW - PW), y, PW, PH);
        } else if (type < 65) {
            p = new BouncyPlatform      (rng.nextFloat() * (GW - PW), y, PW, PH);
        } else if (type < 75) {
            p = new MovingPlatform      (rng.nextFloat() * (GW - PW), y, PW, PH);
        } else if (type < 83) {
            p = new FallingPlatform     (rng.nextFloat() * (GW - PW), y, PW, PH);
        } else if (type < 89) {
            p = new FakePlatform        (rng.nextFloat() * (GW - PW), y, PW, PH);
        } else if (type < 94) {
            p = new SpikePlatform       (rng.nextFloat() * (GW - PW), y, PW, PH);
        } else if (type < 98) {
            p = new MovingEnemy         (rng.nextFloat() * (GW - ENEMY_W), y, ENEMY_W, ENEMY_H);
        } else {
            p = new SpringTrapPlatform  (rng.nextFloat() * (GW - PW), y, PW, PH);
        }
        platforms.add(p);
    }

    private void gameOver() {
        gameState = State.GAME_OVER;
        if (gameOverListener != null) {
            gameOverListener.onGameOver(score);
        }
    }

    void render(Canvas canvas) {
        if (canvas == null) return;

        if (bgImg != null) {
            canvas.drawBitmap(bgImg, srcBgRect, dstBgRect, null);
        }

        canvas.save();
        canvas.scale(scaleX, scaleY);

        for (Platform p : platforms) {
            p.draw(canvas);
        }

        for (PowerUp pu : powerUps) pu.draw(canvas);

        if (slime != null) slime.draw(canvas);

        canvas.restore();

        if (gameState == State.PLAYING) {
            drawHUD(canvas);
        }

        drawPauseButton(canvas);

        if (userPaused) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), overlayPaint);
            float cx = canvas.getWidth() / 2f;
            float cy = canvas.getHeight() / 2f;
            pauseLabelPaint.setTextSize(36f * density);
            canvas.drawText("PAUSED", cx, cy, pauseLabelPaint);
            pauseLabelPaint.setTextSize(16f * density);
            canvas.drawText("Nhan nut  >  de tiep tuc", cx, cy + 52f * density, pauseLabelPaint);
            pauseLabelPaint.setTextSize(36f * density);
        }
    }

    private void drawHUD(Canvas canvas) {
        String scoreText = multiplierTicks > 0 ? "Score: " + score + "  [2x]" : "Score: " + score;
        float dp16 = 16f * density;
        float dp48 = 48f * density;
        canvas.drawText(scoreText, dp16, dp48, scorePaint);

        if (shieldActive) {
            String shieldText = "SHIELD";
            float tw = shieldPaint.measureText(shieldText);
            float pad = 8f * density;
            float x  = canvas.getWidth() - tw - dp16 - (48f + 12f + 12f) * density;
            float y  = dp48;
            canvas.drawRoundRect(x - pad, y - shieldPaint.getTextSize(), x + tw + pad, y + pad / 2f,
                    6f * density, 6f * density, shieldBgPaint);
            canvas.drawText(shieldText, x, y, shieldPaint);
        }
    }

    private void drawPauseButton(Canvas canvas) {
        if (pauseBtnRect == null) return;
        canvas.drawRoundRect(pauseBtnRect, 10f * density, 10f * density, pauseBtnBgPaint);
        String icon = userPaused ? ">" : "II";
        float iconY = pauseBtnRect.centerY() + pauseIconPaint.getTextSize() * 0.35f;
        canvas.drawText(icon, pauseBtnRect.centerX(), iconY, pauseIconPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && pauseBtnRect != null) {
            float extra = 8f * density;
            boolean hit = event.getX() >= pauseBtnRect.left - extra
                    && event.getX() <= pauseBtnRect.right + extra
                    && event.getY() >= pauseBtnRect.top - extra
                    && event.getY() <= pauseBtnRect.bottom + extra;
            if (hit) userPaused = !userPaused;
        }
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorX = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public void pauseGame() {
        if (gameThread != null) gameThread.setPaused(true);
        sensorManager.unregisterListener(this);
    }

    public void resumeGame() {
        if (gameThread == null) return;
        gameThread.setPaused(false);
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
    }

    static class GameThread extends Thread {
        private final SurfaceHolder holder;
        private final GameView view;
        private volatile boolean running = false;
        private volatile boolean paused  = false;
        private static final long FRAME_MS = 16L;

        GameThread(SurfaceHolder holder, GameView view) {
            this.holder = holder;
            this.view   = view;
        }

        void setRunning(boolean r) { running = r; }
        void setPaused(boolean p)  { paused  = p; }

        @Override
        public void run() {
            while (running) {
                if (paused) {
                    try { Thread.sleep(16); } catch (InterruptedException ignored) {}
                    continue;
                }
                long start  = System.currentTimeMillis();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) {
                            view.update();
                            view.render(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas);
                }
                long elapsed = System.currentTimeMillis() - start;
                long sleep   = FRAME_MS - elapsed;
                if (sleep > 0) {
                    try { Thread.sleep(sleep); }
                    catch (InterruptedException ignored) {}
                }
            }
        }
    }
}
