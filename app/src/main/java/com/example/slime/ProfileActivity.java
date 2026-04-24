package com.example.slime;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS = "slime_prefs";
    private static final String KEY_STEPS_ACCUMULATED = "steps_accumulated";
    private static final int STEPS_PER_SHIELD = 50;

    private TextView tvDisplayName, tvUsername, tvHighScore,
            tvGamesPlayed, tvTotalScore, tvAvgScore,
            tvStepsWalked, tvShieldStatus, tvLoadingError;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_profile);

        tvDisplayName = findViewById(R.id.tvProfileDisplayName);
        tvUsername = findViewById(R.id.tvProfileUsername);
        tvHighScore = findViewById(R.id.tvProfileHighScore);
        tvGamesPlayed = findViewById(R.id.tvProfileGamesPlayed);
        tvTotalScore = findViewById(R.id.tvProfileTotalScore);
        tvAvgScore = findViewById(R.id.tvProfileAvgScore);
        tvStepsWalked = findViewById(R.id.tvProfileSteps);
        tvShieldStatus = findViewById(R.id.tvProfileShield);
        tvLoadingError = findViewById(R.id.tvProfileError);
        btnBack = findViewById(R.id.btnProfileBack);

        btnBack.setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        loadLocalStats();
        loadFirestoreStats(user);
    }

    private void loadLocalStats() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long steps = prefs.getLong(KEY_STEPS_ACCUMULATED, 0);
        tvStepsWalked.setText(getString(R.string.profile_steps, steps));
        boolean shieldReady = steps >= STEPS_PER_SHIELD;
        tvShieldStatus.setText(getString(R.string.profile_shield,
                shieldReady ? getString(R.string.shield_ready_label) : getString(R.string.shield_not_ready_label)));
    }

    private void loadFirestoreStats(FirebaseUser user) {
        tvDisplayName.setText(getString(R.string.profile_display_name, user.getDisplayName()));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("uid", user.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        tvUsername.setText(getString(R.string.profile_username, docId));
                    }
                });

        db.collection("leaderboard").document(user.getUid())
                .get()
                .addOnSuccessListener(this::populateStats)
                .addOnFailureListener(e -> {
                    tvLoadingError.setVisibility(View.VISIBLE);
                    tvLoadingError.setText(getString(R.string.profile_load_error));
                });
    }

    private void populateStats(DocumentSnapshot doc) {
        if (!doc.exists()) return;

        long highScore = doc.getLong("score") != null ? doc.getLong("score") : 0;
        long gamesPlayed = doc.getLong("gamesPlayed") != null ? doc.getLong("gamesPlayed") : 0;
        long totalScore = doc.getLong("totalScore") != null ? doc.getLong("totalScore") : 0;
        long avgScore = gamesPlayed > 0 ? totalScore / gamesPlayed : 0;

        tvHighScore.setText(getString(R.string.profile_high_score, highScore));
        tvGamesPlayed.setText(getString(R.string.profile_games_played, gamesPlayed));
        tvTotalScore.setText(getString(R.string.profile_total_score, totalScore));
        tvAvgScore.setText(getString(R.string.profile_avg_score, avgScore));
    }
}
