package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
    private View mainHeader;
    private boolean isFabExpanded = false;

    // Khai báo các biến cho Bottom Navigation (Menu dưới đáy)
    private LinearLayout btnNavHistory, btnNavMap, btnNavNotify, btnNavSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mainHeader = findViewById(R.id.mainHeader);

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

    // --- HÀM XỬ LÝ MENU DƯỚI  ---
    private void initBottomNavigation() {
        // Ánh xạ View
        btnNavHistory = findViewById(R.id.btnNavHistory);
        btnNavMap = findViewById(R.id.btnNavMap);
        btnNavNotify = findViewById(R.id.btnNavNotify);
        btnNavSetting = findViewById(R.id.btnNavSetting);
        mainHeader = findViewById(R.id.mainHeader);

        View fragmentContainer = findViewById(R.id.fragment_container);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();

        // 1. Xử lý nút MAP (Quan trọng nhất: ẨN HEADER ĐỎ)
        btnNavMap.setOnClickListener(v -> {
            // Ẩn cái đầu màu đỏ đi để Map tràn màn hình
            if (mainHeader != null) mainHeader.setVisibility(View.GONE);

            // Xóa khoảng trống trên đầu (marginTop = 0) để Map tràn lên trên
            params.topMargin = 0;
            fragmentContainer.setLayoutParams(params);

            loadFragment(new MapFragment());
        });

        // 2. Xử lý nút HISTORY (Hiện lại Header đỏ)
        btnNavHistory.setOnClickListener(v -> {
            // Hiện lại header đỏ
            if (mainHeader != null) mainHeader.setVisibility(View.VISIBLE);

            int marginInDp = 140;
            params.topMargin = (int) (marginInDp * getResources().getDisplayMetrics().density);
            fragmentContainer.setLayoutParams(params);
            // loadFragment(new HistoryFragment());
        });

        // 3. Xử lý nút NOTIFY (Hiện lại Header đỏ)
        btnNavNotify.setOnClickListener(v -> {
            if (mainHeader != null) mainHeader.setVisibility(View.VISIBLE);

            int marginInDp = 140;
            params.topMargin = (int) (marginInDp * getResources().getDisplayMetrics().density);
            fragmentContainer.setLayoutParams(params);
            // loadFragment(new NotifyFragment());
        });

        // 4. Xử lý nút SETTING (Hiện lại Header đỏ)
        btnNavSetting.setOnClickListener(v -> {
            if (mainHeader != null) mainHeader.setVisibility(View.VISIBLE);

            int marginInDp = 140;
            params.topMargin = (int) (marginInDp * getResources().getDisplayMetrics().density);
            fragmentContainer.setLayoutParams(params);
             loadFragment(new Setting());
        });
    }

    // Hàm hỗ trợ thay đổi Fragment
    private void loadFragment(Fragment fragment) {
        // R.id.fragment_container là FrameLayout trong file XML của bạn
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                //.addToBackStack(null) // Cho phép bấm nút Back để quay lại màn hình trước
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