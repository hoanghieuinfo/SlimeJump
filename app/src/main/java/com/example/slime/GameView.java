package com.example.slime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.slime.entities.SlimeState;
import com.example.slime.platform.BouncyPlatform;
import com.example.slime.platform.DisappearingPlatform;
import com.example.slime.platform.MovingPlatform;
import com.example.slime.platform.Platform;
import com.example.slime.platform.StandardPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView
        implements SurfaceHolder.Callback, SensorEventListener {

    private static final float GW = 400f;   
    private static final float GH = 700f;   

    private static final float GRAVITY   = 0.20f;   
    private static final float INITIAL_DY= -10f;    
    private static final float PW = 68f;   
    private static final float PH = 14f;   
    private static final float SPACING = 80f; 
    private static final int   PLAT_COUNT = 10;
    private static final float WRAP_LEFT  = -50f;
    private static final float WRAP_RIGHT = GW + 50f;
    private static final float SENSOR_SPEED = 0.8f; 

    private enum State { PLAYING, GAME_OVER }
    private State gameState = State.PLAYING;

    private Slime slime;
    private final List<Platform> platforms = new ArrayList<>();
    private final Random rng = new Random();

    private int score = 0;
    private float sensorX = 0f;   
    private SensorManager sensorManager;
    private float scaleX = 1f, scaleY = 1f;

    private SpriteSheet spriteSheet;
    private Paint bgPaint;
    private Paint scorePaint;
    private Paint starPaint;

    private final float[] starX = new float[60];
    private final float[] starY = new float[60];
    private final float[] starR = new float[60];

    private GameThread gameThread;
    
    public interface GameOverListener {
        void onGameOver(int score);
    }
    private GameOverListener gameOverListener;

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        initPaints();
        initStars();
    }

    private void initPaints() {
        bgPaint = new Paint();

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(26f);
        scorePaint.setTypeface(Typeface.DEFAULT_BOLD);
        scorePaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#44000000"));

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(Color.WHITE);
    }

    private void initStars() {
        for (int i = 0; i < starX.length; i++) {
            starX[i] = rng.nextFloat() * GW;
            starY[i] = rng.nextFloat() * GH;
            starR[i] = 0.5f + rng.nextFloat() * 1.5f;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Bitmap raw = BitmapFactory.decodeResource(getResources(), R.drawable.slimejump);
        spriteSheet = new SpriteSheet(raw);

        startGame();

        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }

        gameThread = new GameThread(holder, this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        scaleX = w / GW;
        scaleY = h / GH;
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#0A0A2E"), Color.parseColor("#1A1A5E"),
                          Color.parseColor("#2B1B6F")},
                null, Shader.TileMode.CLAMP));
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
        score = 0;
        gameState = State.PLAYING;

        float cx = GW / 2f - PW / 2f;
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
        if (gameState == State.PLAYING) {
            updatePlaying();
        }
    }

    private void updatePlaying() {
        slime.dx = -sensorX * SENSOR_SPEED;
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
            score += (int)(excess / 5f);
        } else {
            slime.y = newY;
        }

        if (slime.isFalling() && slime.getState() == SlimeState.FALLING) {
            for (Platform p : platforms) {
                if (p.canBounce() && slimeLandsOn(p)) {
                    slime.dy = p.onBounce();  
                    score   += 10;            
                    slime.setState(SlimeState.LANDING); 
                    break;
                }
            }
        }

        slime.updateAnimation();

        for (Platform p : platforms) {
            p.update(GW);
        }

        recycleAndGenerate();

        if (slime.y > GH) {
            gameOver();
        }
    }

    private boolean slimeLandsOn(Platform p) {
        float sl = slime.x + 4f;              
        float sr = slime.x + Slime.SIZE - 4f;
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

    private void spawnPlatform(float y) {
        float x = rng.nextFloat() * (GW - PW);
        int type = rng.nextInt(10);
        Platform p;
        if (type < 6)      p = new StandardPlatform   (x, y, PW, PH);
        else if (type < 8) p = new DisappearingPlatform(x, y, PW, PH);
        else if (type < 9) p = new BouncyPlatform      (x, y, PW, PH);
        else               p = new MovingPlatform      (x, y, PW, PH);
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

        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), bgPaint);

        for (int i = 0; i < starX.length; i++) {
            float alpha = 0.4f + 0.6f * ((float) Math.sin(System.currentTimeMillis() * 0.001f + i) * 0.5f + 0.5f);
            starPaint.setAlpha((int)(alpha * 200));
            canvas.drawCircle(starX[i] * scaleX, starY[i] * scaleY, starR[i] * scaleX, starPaint);
        }

        canvas.save();
        canvas.scale(scaleX, scaleY);

        for (Platform p : platforms) {
            p.draw(canvas);
        }

        if (slime != null) slime.draw(canvas);

        canvas.restore(); 

        if (gameState == State.PLAYING) {
            drawHUD(canvas);     
        }
    }

    private void drawHUD(Canvas canvas) {
        canvas.drawText("Score: " + score, 20f, 60f, scorePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

    static class GameThread extends Thread {
        private final SurfaceHolder holder;
        private final GameView view;
        private volatile boolean running = false;
        private static final long FRAME_MS = 16L; 

        GameThread(SurfaceHolder holder, GameView view) {
            this.holder = holder;
            this.view   = view;
        }

        void setRunning(boolean r) { running = r; }

        @Override
        public void run() {
            while (running) {
                long start = System.currentTimeMillis();
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
