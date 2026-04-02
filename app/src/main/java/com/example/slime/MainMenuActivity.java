package com.example.slime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainMenuActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private Button btnStart, btnLeaderboard, btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_menu);

        tvGreeting = findViewById(R.id.tvGreeting);
        btnStart = findViewById(R.id.btnStart);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, GameActivity.class);
            startActivity(intent);
        });

        btnLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        btnSignIn.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Logout
                FirebaseAuth.getInstance().signOut();
                updateUI();
            } else {
                Intent intent = new Intent(MainMenuActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvGreeting.setVisibility(View.VISIBLE);
            tvGreeting.setText("Hello, " + user.getDisplayName() + "!");
            btnSignIn.setText(getString(R.string.logout));
        } else {
            tvGreeting.setVisibility(View.GONE);
            btnSignIn.setText(getString(R.string.sign_in));
        }
    }
}
