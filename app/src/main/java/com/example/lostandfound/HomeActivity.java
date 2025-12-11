package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeActivity extends AppCompatActivity {

    // --- CẬP NHẬT BIẾN CHO FAB MỚI ---
    private FloatingActionButton fabCreatePost, fabHome, fabLost, fabFound; // fabAdd đổi tên thành fabCreatePost
    private TextView tvLostLabel, tvFoundLabel;
    private View mainHeader;
    private boolean isFabExpanded = false;

    // Khai báo các biến cho Bottom Navigation
    private LinearLayout btnNavHistory, btnNavMap, btnNavNotify, btnNavSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mainHeader = findViewById(R.id.mainHeader);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initFabMenu();
        initBottomNavigation();

        // Mặc định load trang tin tức
        if (savedInstanceState == null) {
            loadFragment(new NewsFeedFragment()); // Chú ý: Tên class là NewFeedFragment (theo code cũ của bạn)
        }
    }

    private void initBottomNavigation() {
        btnNavHistory = findViewById(R.id.btnNavHistory);
        btnNavMap = findViewById(R.id.btnNavMap);
        btnNavNotify = findViewById(R.id.btnNavNotify);
        btnNavSetting = findViewById(R.id.btnNavSetting);
        mainHeader = findViewById(R.id.mainHeader);

        View fragmentContainer = findViewById(R.id.fragment_container);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();

        // Nút Map: Ẩn Header
        btnNavMap.setOnClickListener(v -> {
            if (mainHeader != null) mainHeader.setVisibility(View.GONE);
            params.topMargin = 0;
            fragmentContainer.setLayoutParams(params);
            loadFragment(new MapFragment());
        });

        // Hàm phụ để reset giao diện về mặc định (Hiện header)
        View.OnClickListener defaultNavAction = v -> {
            if (mainHeader != null) mainHeader.setVisibility(View.VISIBLE);
            int marginInDp = 140;
            params.topMargin = (int) (marginInDp * getResources().getDisplayMetrics().density);
            fragmentContainer.setLayoutParams(params);
        };

        btnNavHistory.setOnClickListener(v -> {
            defaultNavAction.onClick(v);
            // loadFragment(new HistoryFragment());
        });

        btnNavNotify.setOnClickListener(v -> {
            defaultNavAction.onClick(v);
            // loadFragment(new NotifyFragment());
        });

        btnNavSetting.setOnClickListener(v -> {
            defaultNavAction.onClick(v);
            loadFragment(new Setting()); // Sửa lại tên class cho đúng SettingFragment
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // --- LOGIC FAB MỚI (Bay dọc) ---
    private void initFabMenu() {
        // 1. Ánh xạ nút mới
        fabCreatePost = findViewById(R.id.fabCreatePost); // Nút + đỏ ở góc
        fabHome = findViewById(R.id.fabHome);             // Nút ngôi nhà ở giữa

        fabLost = findViewById(R.id.fabLost);
        fabFound = findViewById(R.id.fabFound);
        tvLostLabel = findViewById(R.id.tvLostLabel);
        tvFoundLabel = findViewById(R.id.tvFoundLabel);

        // 2. Xử lý nút HOME (Về trang chủ)
        fabHome.setOnClickListener(v -> {
            if (isFabExpanded) closeFabMenu();

            // Reset giao diện (Hiện header)
            if (mainHeader != null) mainHeader.setVisibility(View.VISIBLE);
            View fragmentContainer = findViewById(R.id.fragment_container);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
            int marginInDp = 140;
            params.topMargin = (int) (marginInDp * getResources().getDisplayMetrics().density);
            fragmentContainer.setLayoutParams(params);

            loadFragment(new NewsFeedFragment());
        });

        // 3. Xử lý nút Đăng bài (+)
        fabCreatePost.setOnClickListener(v -> {
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

        fabFound.animate().translationX(-170f).translationY(0f);
        tvFoundLabel.animate().translationX(-170f).translationY(0f);

        fabLost.animate().translationX(0f).translationY(-170f);
        tvLostLabel.animate().translationX(0f).translationY(-170f);

        // Xoay nút cộng thành dấu X
        fabCreatePost.animate().rotation(45f);
    }

    private void closeFabMenu() {
        isFabExpanded = false;

        // Thu nút FOUND về (Reset Y)
        fabFound.animate().translationX(0f).translationY(0f);
        tvFoundLabel.animate().translationX(0f).translationY(0f);

        // Thu nút LOST về (Reset cả X và Y)
        fabLost.animate().translationY(0).translationX(0);
        tvLostLabel.animate().translationY(0).translationX(0);


        // Ẩn đi sau khi animation xong
        fabLost.animate().withEndAction(() -> fabLost.setVisibility(View.INVISIBLE));
        fabFound.animate().withEndAction(() -> fabFound.setVisibility(View.INVISIBLE));
        tvLostLabel.animate().withEndAction(() -> tvLostLabel.setVisibility(View.INVISIBLE));
        tvFoundLabel.animate().withEndAction(() -> tvFoundLabel.setVisibility(View.INVISIBLE));

        // Xoay về dấu +
        fabCreatePost.animate().rotation(0);
    }
}