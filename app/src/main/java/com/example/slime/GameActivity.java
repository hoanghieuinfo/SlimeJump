package com.example.slime;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import android.content.Intent;

public class GameActivity extends Activity implements GameView.GameOverListener {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen, no title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gameView = new GameView(this);
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
        // GameView unregisters sensor in surfaceDestroyed
    }

    @Override
    protected void onResume() {
        super.onResume();
        // GameView re-registers sensor when surface is recreated
    }
}
