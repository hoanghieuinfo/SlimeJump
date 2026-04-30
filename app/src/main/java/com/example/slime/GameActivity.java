package com.example.slime;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import android.content.Intent;
import com.example.slime.entities.BackgroundTheme;

public class GameActivity extends Activity implements GameView.GameOverListener {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String themeStr = getIntent().getStringExtra("BG_THEME");
        BackgroundTheme theme = BackgroundTheme.DAY;
        if (themeStr != null) {
            try {
                theme = BackgroundTheme.valueOf(themeStr);
            } catch (Exception ignored) { }
        }

        boolean hasShield = getIntent().getBooleanExtra("HAS_SHIELD", false);

        gameView = new GameView(this, theme, hasShield);
        gameView.setGameOverListener(this);
        setContentView(gameView);
    }

    @Override
    public void onGameOver(int score) {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, GameOverActivity.class);
            intent.putExtra("SCORE", score);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.resumeGame();
    }
}
