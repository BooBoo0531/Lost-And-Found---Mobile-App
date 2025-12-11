package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignUp, tvForgotPassword;

    // 1. Khai báo Firebase Auth
    private FirebaseAuth mAuth;

    @Override
    protected void onStart() {
        super.onStart();
        // 2. AUTO LOGIN: Kiểm tra nếu user đã đăng nhập từ trước thì vào thẳng Home
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            goToHomeActivity();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 3. Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Handle notch (Giữ nguyên code của bạn)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mapping view
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Login button
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim(); // Thêm trim() để xoá khoảng trắng thừa
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng điền đầy đủ email và mật khẩu!", Toast.LENGTH_SHORT).show();
            } else {
                performLogin(email, password);
            }
        });

        // Sign Up navigation
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Forgot Password navigation
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin(String email, String pass) {
        // 4. Gọi Firebase để đăng nhập thật
        // Hiển thị thông báo đang xử lý (có thể thêm ProgressBar nếu muốn)
        Toast.makeText(this, "Đang xử lý đăng nhập...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công -> Vào Home
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        goToHomeActivity();
                    } else {
                        // Đăng nhập thất bại -> Hiện lỗi
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                        Toast.makeText(LoginActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hàm chuyển trang chung để dùng lại (DRY)
    private void goToHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        // 5. CỜ (FLAGS) QUAN TRỌNG: Xóa sạch lịch sử để không back lại được Login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}