package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeActivity extends AppCompatActivity {

    // Khai báo các biến cho FAB (Nút tròn)
    private FloatingActionButton fabAdd, fabLost, fabFound;
    private TextView tvLostLabel, tvFoundLabel;
    private boolean isFabExpanded = false;

    // Khai báo các biến cho Bottom Navigation (Menu dưới đáy)
    private LinearLayout btnNavHistory, btnNavMap, btnNavNotify, btnNavSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Xử lý giao diện tràn viền (EdgeToEdge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. KHỞI TẠO CÁC NÚT FAB (Đăng bài)
        initFabMenu();

        // 2. KHỞI TẠO MENU DƯỚI (Điều hướng Map, History...)
        initBottomNavigation();
        if (savedInstanceState == null) {
            loadFragment(new NewsFeedFragment());
        }
    }

    // --- HÀM XỬ LÝ MENU DƯỚI (MỚI THÊM) ---
    private void initBottomNavigation() {
        // Ánh xạ View từ layout XML
        btnNavHistory = findViewById(R.id.btnNavHistory);
        btnNavMap = findViewById(R.id.btnNavMap);
        btnNavNotify = findViewById(R.id.btnNavNotify);
        btnNavSetting = findViewById(R.id.btnNavSetting);

        // Xử lý sự kiện bấm nút Map
        btnNavMap.setOnClickListener(v -> {
            // Gọi MapFragment hiện lên màn hình
            loadFragment(new MapFragment());
        });

        // Xử lý sự kiện bấm nút History (Ví dụ mẫu, bạn có thể tạo Fragment sau)
        btnNavHistory.setOnClickListener(v -> {
            // loadFragment(new HistoryFragment()); // Bỏ comment khi bạn đã tạo HistoryFragment
        });

        // Tương tự cho Notify và Setting...
    }

    // Hàm hỗ trợ thay đổi Fragment
    private void loadFragment(Fragment fragment) {
        // R.id.fragment_container là FrameLayout trong file XML của bạn
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // Cho phép bấm nút Back để quay lại màn hình trước
                .commit();
    }

    // --- HÀM XỬ LÝ FAB (CODE CŨ CỦA BẠN) ---
    private void initFabMenu() {
        fabAdd = findViewById(R.id.fabAdd);
        fabLost = findViewById(R.id.fabLost);
        fabFound = findViewById(R.id.fabFound);
        tvLostLabel = findViewById(R.id.tvLostLabel);
        tvFoundLabel = findViewById(R.id.tvFoundLabel);

        fabAdd.setOnClickListener(v -> {
            if (isFabExpanded) closeFabMenu();
            else openFabMenu();
        });

        fabLost.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(HomeActivity.this, PostActivity.class);
            intent.putExtra("POST_TYPE", "LOST");
            startActivity(intent);
        });

        fabFound.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(HomeActivity.this, PostActivity.class);
            intent.putExtra("POST_TYPE", "FOUND");
            startActivity(intent);
        });
    }

    private void openFabMenu() {
        isFabExpanded = true;
        fabLost.setVisibility(View.VISIBLE);
        fabFound.setVisibility(View.VISIBLE);
        tvLostLabel.setVisibility(View.VISIBLE);
        tvFoundLabel.setVisibility(View.VISIBLE);

        fabLost.animate().translationX(-200f).translationY(-160f);
        tvLostLabel.animate().translationX(-200f).translationY(-210f);

        fabFound.animate().translationX(200f).translationY(-160f);
        tvFoundLabel.animate().translationX(200f).translationY(-210f);

        fabAdd.animate().rotation(45f);
    }

    private void closeFabMenu() {
        isFabExpanded = false;

        fabLost.animate().translationX(0).translationY(0);
        tvLostLabel.animate().translationX(0).translationY(0);

        fabFound.animate().translationX(0).translationY(0);
        tvFoundLabel.animate().translationX(0).translationY(0);

        fabLost.animate().withEndAction(() -> fabLost.setVisibility(View.INVISIBLE));
        fabFound.animate().withEndAction(() -> fabFound.setVisibility(View.INVISIBLE));
        tvLostLabel.animate().withEndAction(() -> tvLostLabel.setVisibility(View.INVISIBLE));
        tvFoundLabel.animate().withEndAction(() -> tvFoundLabel.setVisibility(View.INVISIBLE));

        fabAdd.animate().rotation(0);
    }
}