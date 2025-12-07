package com.example.lostandfound;

import android.content.Intent; // Import để chuyển màn hình
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // 1. Khai báo biến
    private ImageView btnNext;
    private TextView tvContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kích hoạt chế độ tràn viền
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // Xử lý khoảng cách hệ thống (Status bar, Navigation bar) để không bị che nội dung
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Ánh xạ ID (Tìm view trong layout)
        btnNext = findViewById(R.id.btnNext);
        tvContinue = findViewById(R.id.tvContinue);

        // Kiểm tra null để tránh lỗi nếu quên thêm ID trong XML
        if (btnNext != null && tvContinue != null) {

            // 3. Tạo sự kiện click chung
            View.OnClickListener continueListener = v -> navigateToNextScreen();

            btnNext.setOnClickListener(continueListener);
            tvContinue.setOnClickListener(continueListener);
        }
    }

    // Hàm xử lý chuyển màn hình
    private void navigateToNextScreen() {
        Toast.makeText(this, "Clicked Continue!", Toast.LENGTH_SHORT).show();

        // --- Mở màn hình tiếp theo (Ví dụ: LoginActivity) ---
        // Khi bạn tạo xong màn hình Login, hãy bỏ comment 2 dòng dưới:

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}