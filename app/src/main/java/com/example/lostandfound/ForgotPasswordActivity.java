package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etNewPassword, etConfirmPassword;
    private Button btnUpdatePassword;
    private TextView tvGoBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.etEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        tvGoBack = findViewById(R.id.tvGoBack);

        tvGoBack.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });

        btnUpdatePassword.setOnClickListener(v -> validateAndUpdate());
    }

    private void validateAndUpdate() {

        String email = etEmail.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(newPass)) {
            etNewPassword.setError("Password cannot be empty");
            return;
        }

        if (newPass.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();

        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
        finish();
    }
}
