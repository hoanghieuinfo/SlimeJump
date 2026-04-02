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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etDisplayName;
    private Button btnSubmit;
    private TextView tvSignIn, tvError;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvSignIn = findViewById(R.id.tvSignIn);
        tvError = findViewById(R.id.tvError);

        btnSubmit.setOnClickListener(v -> registerUser());

        tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String displayName = etDisplayName.getText().toString().trim();

        if (!validateInputs(username, password, displayName)) {
            return;
        }

        tvError.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);

        // 1. Check if username is unique in Firestore
        String usernameLower = username.toLowerCase();
        db.collection("users").document(usernameLower).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().exists()) {
                            showError("Tên tài khoản đã tồn tại");
                            btnSubmit.setEnabled(true);
                        } else {
                            // 2. Proceed with Firebase Auth registration
                            proceedWithAuth(username, usernameLower, password, displayName);
                        }
                    } else {
                        showError("Lỗi kết nối CSDL: " + task.getException().getMessage());
                        btnSubmit.setEnabled(true);
                    }
                });
    }

    private boolean validateInputs(String username, String password, String displayName) {
        // tên tài khoản: 3-16 chars, no spaces, no special (only letters/numbers)
        if (!Pattern.matches("^[a-zA-Z0-9]{3,16}$", username)) {
            showError("Tài khoản từ 3-16 kí tự, không có kí tự đặc biệt và khoảng trắng");
            return false;
        }

        // mật khẩu: ít nhất 8 kí tự, không kí tự đặc biệt
        if (!Pattern.matches("^[a-zA-Z0-9]{8,}$", password)) {
            showError("Mật khẩu ít nhất 8 kí tự, không có kí tự đặc biệt và khoảng trắng");
            return false;
        }

        // display name: 3-16 kí tự, có khoảng trống, có 3 kí tự đặc biệt !@_
        if (!Pattern.matches("^[a-zA-Z0-9!@_ ]{3,16}$", displayName)) {
            showError("Tên hiển thị từ 3-16 kí tự, chỉ chứa (!, @, _) và khoảng trắng");
            return false;
        }

        return true;
    }

    private void proceedWithAuth(String origUsername, String usernameLower, String password, String displayName) {
        String dummyEmail = origUsername + "@slime.game";

        mAuth.createUserWithEmailAndPassword(dummyEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            updateProfileAndFirestore(user, usernameLower, displayName);
                        }
                    } else {
                        btnSubmit.setEnabled(true);
                        showError("Đăng ký thất bại: " + task.getException().getMessage());
                    }
                });
    }

    private void updateProfileAndFirestore(FirebaseUser user, String usernameLower, String displayName) {
        // Set Firebase Auth Display name
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();
        user.updateProfile(profileUpdates);

        // Claim username in "users" collection
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());

        db.collection("users").document(usernameLower).set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Create leaderboard entry
                    Map<String, Object> scoreData = new HashMap<>();
                    scoreData.put("score", 0);
                    scoreData.put("displayName", displayName);
                    scoreData.put("lastUpdated", System.currentTimeMillis());

                    db.collection("leaderboard").document(user.getUid()).set(scoreData)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                finish(); // Finish registration, user is now logged in
                            });
                });
    }

    private void showError(String message) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }
}
