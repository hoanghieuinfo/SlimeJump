package com.example.slime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnSubmit;
    private TextView tvSignUp, tvError;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvError = findViewById(R.id.tvError);

        btnSubmit.setOnClickListener(v -> loginUser());

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng điền đủ thông tin");
            return;
        }

        tvError.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);
        
        // Map username to dummy email
        String dummyEmail = username + "@slime.game";

        mAuth.signInWithEmailAndPassword(dummyEmail, password)
                .addOnCompleteListener(this, task -> {
                    btnSubmit.setEnabled(true);
                    if (task.isSuccessful()) {
                        syncScoreToLocal(task.getResult().getUser());
                    } else {
                        showError("Sai tài khoản hoặc mật khẩu");
                    }
                });
    }

    private void syncScoreToLocal(FirebaseUser user) {
        if (user == null) {
            finish();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("leaderboard").document(user.getUid()).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Long scoreObj = task.getResult().getLong("score");
                    int score = scoreObj != null ? scoreObj.intValue() : 0;
                    getSharedPreferences("slime_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putInt("hi_score_" + user.getUid(), score)
                            .apply();
                }
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                finish(); // return to main menu
            });
    }

    private void showError(String message) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }
}
