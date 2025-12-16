package com.example.lostandfound;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeActivity extends AppCompatActivity {

    private FloatingActionButton fabCreatePost, fabHome, fabLost, fabFound;
    private TextView tvLostLabel, tvFoundLabel;
    private boolean isFabExpanded = false;

    private LinearLayout btnNavHistory, btnNavMap, btnNavNotify, btnNavSetting;

    private AppBarLayout appBarLayout;
    private View btnSearchSmall;

    // ✅ ViewModel + Launcher
    private SharedPostViewModel postVM;
    private ActivityResultLauncher<Intent> createPostLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // ✅ init ViewModel
        postVM = new ViewModelProvider(this).get(SharedPostViewModel.class);

        // ✅ nhận NEW_POST từ PostActivity
        createPostLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Post newPost = (Post) result.getData().getSerializableExtra("NEW_POST");
                        if (newPost != null) {
                            postVM.addPost(newPost); // ✅ MapFragment đang observe sẽ update ngay
                        }
                    }
                }
        );

        appBarLayout = findViewById(R.id.appBarLayout);
        btnSearchSmall = findViewById(R.id.btnSearchSmall);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupCollapsingHeader();
        initFabMenu();
        initBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new NewsFeedFragment());
        }
    }

    private void setupCollapsingHeader() {
        btnSearchSmall.setOnClickListener(v ->
                Toast.makeText(this, "Đang mở tìm kiếm...", Toast.LENGTH_SHORT).show()
        );

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
            if (appBarLayout != null) appBarLayout.setExpanded(false, true);
            loadFragment(new MapFragment());
        });

        btnNavSetting.setOnClickListener(v -> {
            if (appBarLayout != null) appBarLayout.setExpanded(false, true);
            loadFragment(new Setting());
        });

        btnNavHistory.setOnClickListener(v -> {
            if (appBarLayout != null) appBarLayout.setExpanded(true, true);
            loadFragment(new HistoryFragment());
        });

        btnNavNotify.setOnClickListener(v -> {
            if (appBarLayout != null) appBarLayout.setExpanded(true, true);
            // loadFragment(new NotifyFragment());
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
            if (appBarLayout != null) appBarLayout.setExpanded(true, true);
            loadFragment(new NewsFeedFragment());
        });

        fabCreatePost.setOnClickListener(v -> {
            if (isFabExpanded) closeFabMenu();
            else openFabMenu();
        });

        // ✅ CHỖ NÀY: thay startActivity -> createPostLauncher.launch
        fabLost.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(HomeActivity.this, PostActivity.class);
            intent.putExtra("POST_TYPE", "LOST");
            createPostLauncher.launch(intent);
        });

        fabFound.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(HomeActivity.this, PostActivity.class);
            intent.putExtra("POST_TYPE", "FOUND");
            createPostLauncher.launch(intent);
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
