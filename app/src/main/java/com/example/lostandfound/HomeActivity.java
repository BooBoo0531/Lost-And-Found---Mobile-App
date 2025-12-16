package com.example.lostandfound;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends AppCompatActivity {

    // --- BIẾN FAB ---
    private FloatingActionButton fabCreatePost, fabHome, fabLost, fabFound;
    private TextView tvLostLabel, tvFoundLabel;
    private boolean isFabExpanded = false;

    // --- BIẾN ĐIỀU HƯỚNG ---
    private LinearLayout btnNavHistory, btnNavMap, btnNavNotify, btnNavSetting;

    // --- BIẾN HEADER ---
    private AppBarLayout appBarLayout;
    private View btnSearchSmall;
    private View fragmentContainer;

    private ImageView imgUserAvatarHome;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        try {
            mDatabase = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 1. Ánh xạ
        appBarLayout = findViewById(R.id.appBarLayout);
        btnSearchSmall = findViewById(R.id.btnSearchSmall);
        fragmentContainer = findViewById(R.id.fragment_container);


        // Xử lý EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupCollapsingHeader();
        initFabMenu();
        initBottomNavigation();


        // Mặc định load trang Home
        if (savedInstanceState == null) {
            loadFragment(new NewsFeedFragment());
            updateHeaderVisibility(true);
        }
    }

    // --- HÀM ẨN/HIỆN HEADER ---
    private void updateHeaderVisibility(boolean isVisible) {
        if (appBarLayout == null || fragmentContainer == null) return;

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) fragmentContainer.getLayoutParams();

        if (isVisible) {
            params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
            appBarLayout.setVisibility(View.VISIBLE);
            appBarLayout.setExpanded(true, true);
        } else {
            params.setBehavior(null);
            appBarLayout.setVisibility(View.GONE);
        }
        fragmentContainer.requestLayout();
    }

    private void setupCollapsingHeader() {
        btnSearchSmall.setOnClickListener(v -> {
            Toast.makeText(this, "Đang mở tìm kiếm...", Toast.LENGTH_SHORT).show();
        });

        appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
            float percentage = (float) Math.abs(verticalOffset) / appBar.getTotalScrollRange();
            if (percentage > 0.75f) {
                if (btnSearchSmall.getVisibility() != View.VISIBLE) {
                    btnSearchSmall.setVisibility(View.VISIBLE);
                    btnSearchSmall.animate().alpha(1f).setDuration(200).start();
                }
            } else {
                if (btnSearchSmall.getVisibility() == View.VISIBLE) {
                    btnSearchSmall.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void initBottomNavigation() {
        btnNavHistory = findViewById(R.id.btnNavHistory);
        btnNavMap = findViewById(R.id.btnNavMap);
        btnNavNotify = findViewById(R.id.btnNavNotify);
        btnNavSetting = findViewById(R.id.btnNavSetting);

        btnNavMap.setOnClickListener(v -> {
            loadFragment(new MapFragment());
            updateHeaderVisibility(false);
        });

        btnNavSetting.setOnClickListener(v -> {
            loadFragment(new Setting());
            updateHeaderVisibility(false);
        });

        btnNavHistory.setOnClickListener(v -> {
            // loadFragment(new HistoryFragment());
            updateHeaderVisibility(true);
        });

        btnNavNotify.setOnClickListener(v -> {
            // loadFragment(new NotifyFragment());
            updateHeaderVisibility(true);
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void initFabMenu() {
        fabCreatePost = findViewById(R.id.fabCreatePost);
        fabHome = findViewById(R.id.fabHome);
        fabLost = findViewById(R.id.fabLost);
        fabFound = findViewById(R.id.fabFound);
        tvLostLabel = findViewById(R.id.tvLostLabel);
        tvFoundLabel = findViewById(R.id.tvFoundLabel);

        fabHome.setOnClickListener(v -> {
            if (isFabExpanded) closeFabMenu();
            loadFragment(new NewsFeedFragment());
            updateHeaderVisibility(true);
        });

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

        fabCreatePost.animate().rotation(45f);
    }

    private void closeFabMenu() {
        isFabExpanded = false;

        fabFound.animate().translationX(0f).translationY(0f);
        tvFoundLabel.animate().translationX(0f).translationY(0f);
        fabLost.animate().translationX(0f).translationY(0f);
        tvLostLabel.animate().translationX(0f).translationY(0f);

        fabLost.animate().withEndAction(() -> fabLost.setVisibility(View.INVISIBLE));
        fabFound.animate().withEndAction(() -> fabFound.setVisibility(View.INVISIBLE));
        tvLostLabel.animate().withEndAction(() -> tvLostLabel.setVisibility(View.INVISIBLE));
        tvFoundLabel.animate().withEndAction(() -> tvFoundLabel.setVisibility(View.INVISIBLE));

        fabCreatePost.setImageResource(R.drawable.ic_add);
        fabCreatePost.setBackgroundResource(0);
        fabCreatePost.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F07A7A")));
        fabCreatePost.setRotation(0f);
    }
}