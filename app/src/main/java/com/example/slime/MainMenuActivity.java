package com.example.slime;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.slime.entities.BackgroundTheme;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainMenuActivity extends AppCompatActivity implements SensorEventListener {

    private static final String PREFS = "slime_prefs";
    private static final String KEY_STEPS_CHECKPOINT = "steps_checkpoint";
    private static final String KEY_STEPS_ACCUMULATED = "steps_accumulated";
    private static final int STEPS_PER_SHIELD = 50;
    private static final int REQUEST_ACTIVITY_RECOGNITION = 101;

    private TextView tvGreeting, tvStepInfo;
    private Button btnStart, btnLeaderboard, btnSignIn, btnProfile;

    private ImageView bgImageView;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor stepCounterSensor;
    private BackgroundTheme currentTheme = BackgroundTheme.DAY;

    private long sessionStartSteps = -1;
    private long currentStepReading = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_menu);

        bgImageView = findViewById(R.id.mainMenuBg);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvStepInfo = findViewById(R.id.tvStepInfo);
        btnStart = findViewById(R.id.btnStart);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnProfile = findViewById(R.id.btnProfile);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, GameActivity.class);
            intent.putExtra("BG_THEME", currentTheme.name());
            intent.putExtra("HAS_SHIELD", hasShieldAvailable());
            startActivity(intent);
        });

        btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(MainMenuActivity.this, LeaderboardActivity.class)));

        btnProfile.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                startActivity(new Intent(MainMenuActivity.this, ProfileActivity.class));
            } else {
                startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
            }
        });

        btnSignIn.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseAuth.getInstance().signOut();
                updateUI();
            } else {
                startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
            }
        });

        requestActivityRecognitionPermission();
    }

    private void requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        REQUEST_ACTIVITY_RECOGNITION);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveStepsOnPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        sessionStartSteps = -1;
    }

    private void saveStepsOnPause() {
        if (sessionStartSteps < 0 || currentStepReading < 0) return;
        long delta = currentStepReading - sessionStartSteps;
        if (delta <= 0) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long accumulated = prefs.getLong(KEY_STEPS_ACCUMULATED, 0) + delta;
        prefs.edit()
                .putLong(KEY_STEPS_ACCUMULATED, accumulated)
                .putLong(KEY_STEPS_CHECKPOINT, currentStepReading)
                .apply();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            BackgroundTheme newTheme = (lux > 40) ? BackgroundTheme.DAY : BackgroundTheme.NIGHT;
            if (newTheme != currentTheme) {
                currentTheme = newTheme;
                bgImageView.setImageResource(
                        currentTheme == BackgroundTheme.DAY ? R.drawable.cloudsday : R.drawable.cloudsnight);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            long totalSteps = (long) event.values[0];
            if (sessionStartSteps < 0) {
                sessionStartSteps = totalSteps;
            }
            currentStepReading = totalSteps;
            updateStepDisplay();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateStepDisplay() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long accumulated = prefs.getLong(KEY_STEPS_ACCUMULATED, 0);

        long sessionSteps = 0;
        if (sessionStartSteps >= 0 && currentStepReading >= sessionStartSteps) {
            sessionSteps = currentStepReading - sessionStartSteps;
        }
        long totalSteps = accumulated + sessionSteps;
        long stepsToNextShield = STEPS_PER_SHIELD - (totalSteps % STEPS_PER_SHIELD);

        if (hasShieldAvailable()) {
            tvStepInfo.setText(getString(R.string.shield_ready, totalSteps));
        } else {
            tvStepInfo.setText(getString(R.string.steps_to_shield, totalSteps, stepsToNextShield));
        }
        tvStepInfo.setVisibility(View.VISIBLE);
    }

    private boolean hasShieldAvailable() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long accumulated = prefs.getLong(KEY_STEPS_ACCUMULATED, 0);

        long sessionSteps = 0;
        if (sessionStartSteps >= 0 && currentStepReading >= sessionStartSteps) {
            sessionSteps = currentStepReading - sessionStartSteps;
        }
        long totalSteps = accumulated + sessionSteps;
        return totalSteps >= STEPS_PER_SHIELD;
    }

    private void updateUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvGreeting.setVisibility(View.VISIBLE);
            tvGreeting.setText(getString(R.string.greeting, user.getDisplayName()));
            btnSignIn.setText(getString(R.string.logout));
            btnProfile.setVisibility(View.VISIBLE);
        } else {
            tvGreeting.setVisibility(View.GONE);
            btnSignIn.setText(getString(R.string.sign_in));
            btnProfile.setVisibility(View.GONE);
        }
        if (stepCounterSensor == null) {
            tvStepInfo.setVisibility(View.GONE);
        }
    }
}
