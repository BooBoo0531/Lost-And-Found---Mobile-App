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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends AppCompatActivity {

    // ===== UI =====
    private FloatingActionButton fabCreatePost, fabHome, fabLost, fabFound;
    private TextView tvLostLabel, tvFoundLabel;
    private boolean isFabExpanded = false;

    private LinearLayout btnNavHistory, btnNavMap, btnNavNotify, btnNavSetting;
    private View viewNotifyDot;

    private AppBarLayout appBarLayout;
    private View btnSearchSmall;

    // ===== ViewModel =====
    private SharedPostViewModel postVM;

    // ===== Create post launcher =====
    private ActivityResultLauncher<Intent> createPostLauncher;

    // ===== Notify badge listener =====
    private static final String DB_URL =
            "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    private DatabaseReference notifyRef;
    private Query unreadQuery;
    private ValueEventListener unreadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        postVM = new ViewModelProvider(this).get(SharedPostViewModel.class);

        createPostLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Post newPost = (Post) result.getData().getSerializableExtra("NEW_POST");
                            if (newPost != null) postVM.addPost(newPost);
                        } catch (Exception ignored) {}
                    }
                }
        );

        appBarLayout = findViewById(R.id.appBarLayout);
        btnSearchSmall = findViewById(R.id.btnSearchSmall);

        // Insets (notch)
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
        if (btnSearchSmall != null) {
            btnSearchSmall.setOnClickListener(v ->
                    Toast.makeText(this, "Đang mở tìm kiếm...", Toast.LENGTH_SHORT).show()
            );
        }

        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
                float percentage = (float) Math.abs(verticalOffset) / appBar.getTotalScrollRange();
                if (percentage > 0.75f) {
                    if (btnSearchSmall != null && btnSearchSmall.getVisibility() != View.VISIBLE) {
                        btnSearchSmall.setVisibility(View.VISIBLE);
                        btnSearchSmall.animate().alpha(1f).setDuration(200).start();
                    }
                } else {
                    if (btnSearchSmall != null && btnSearchSmall.getVisibility() == View.VISIBLE) {
                        btnSearchSmall.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    }

    private void initBottomNavigation() {
        btnNavHistory = findViewById(R.id.btnNavHistory);
        btnNavMap = findViewById(R.id.btnNavMap);
        btnNavNotify = findViewById(R.id.btnNavNotify);
        btnNavSetting = findViewById(R.id.btnNavSetting);

        viewNotifyDot = findViewById(R.id.viewNotifyDot);

        if (btnNavMap != null) {
            btnNavMap.setOnClickListener(v -> {
                if (appBarLayout != null) appBarLayout.setExpanded(false, true);
                loadFragment(new MapFragment());
            });
        }

        if (btnNavSetting != null) {
            btnNavSetting.setOnClickListener(v -> {
                if (appBarLayout != null) appBarLayout.setExpanded(false, true);
                loadFragment(new Setting());
            });
        }

        if (btnNavHistory != null) {
            btnNavHistory.setOnClickListener(v -> {
                if (appBarLayout != null) appBarLayout.setExpanded(true, true);
                loadFragment(new HistoryFragment());
            });
        }

        if (btnNavNotify != null) {
            btnNavNotify.setOnClickListener(v -> {
                if (appBarLayout != null) appBarLayout.setExpanded(true, true);
                loadFragment(new NotificationFragment());
            });
        }
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

        if (fabHome != null) {
            fabHome.setOnClickListener(v -> {
                if (isFabExpanded) closeFabMenu();
                if (appBarLayout != null) appBarLayout.setExpanded(true, true);
                loadFragment(new NewsFeedFragment());
            });
        }

        if (fabCreatePost != null) {
            fabCreatePost.setOnClickListener(v -> {
                if (isFabExpanded) closeFabMenu();
                else openFabMenu();
            });
        }

        if (fabLost != null) {
            fabLost.setOnClickListener(v -> {
                closeFabMenu();
                Intent intent = new Intent(HomeActivity.this, PostActivity.class);
                intent.putExtra("POST_TYPE", "LOST");
                createPostLauncher.launch(intent);
            });
        }

        if (fabFound != null) {
            fabFound.setOnClickListener(v -> {
                closeFabMenu();
                Intent intent = new Intent(HomeActivity.this, PostActivity.class);
                intent.putExtra("POST_TYPE", "FOUND");
                createPostLauncher.launch(intent);
            });
        }
    }

    private void openFabMenu() {
        isFabExpanded = true;

        if (fabLost != null) fabLost.setVisibility(View.VISIBLE);
        if (fabFound != null) fabFound.setVisibility(View.VISIBLE);
        if (tvLostLabel != null) tvLostLabel.setVisibility(View.VISIBLE);
        if (tvFoundLabel != null) tvFoundLabel.setVisibility(View.VISIBLE);

        if (fabFound != null) fabFound.animate().translationX(-170f).translationY(0f);
        if (tvFoundLabel != null) tvFoundLabel.animate().translationX(-170f).translationY(0f);
        if (fabLost != null) fabLost.animate().translationX(0f).translationY(-170f);
        if (tvLostLabel != null) tvLostLabel.animate().translationX(0f).translationY(-170f);

        if (fabCreatePost != null) fabCreatePost.animate().rotation(45f);
    }

    private void closeFabMenu() {
        isFabExpanded = false;

        if (fabFound != null) fabFound.animate().translationX(0f).translationY(0f);
        if (tvFoundLabel != null) tvFoundLabel.animate().translationX(0f).translationY(0f);
        if (fabLost != null) fabLost.animate().translationX(0f).translationY(0f);
        if (tvLostLabel != null) tvLostLabel.animate().translationX(0f).translationY(0f);

        if (fabLost != null) fabLost.animate().withEndAction(() -> fabLost.setVisibility(View.INVISIBLE));
        if (fabFound != null) fabFound.animate().withEndAction(() -> fabFound.setVisibility(View.INVISIBLE));
        if (tvLostLabel != null) tvLostLabel.animate().withEndAction(() -> tvLostLabel.setVisibility(View.INVISIBLE));
        if (tvFoundLabel != null) tvFoundLabel.animate().withEndAction(() -> tvFoundLabel.setVisibility(View.INVISIBLE));

        if (fabCreatePost != null) {
            fabCreatePost.setImageResource(R.drawable.ic_add);
            fabCreatePost.setBackgroundResource(0);
            fabCreatePost.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F07A7A")));
            fabCreatePost.setRotation(0f);
        }
    }

    // ===== Notify badge =====
    private void startNotifyBadgeListener() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            setNotifyDotVisible(false);
            return;
        }

        notifyRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("notifications")
                .child(uid);

        // gỡ listener cũ
        if (unreadListener != null && notifyRef != null) {
            notifyRef.removeEventListener(unreadListener);
        }

        unreadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasUnread = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean isRead = child.child("isRead").getValue(Boolean.class);
                    if (isRead == null || !isRead) { // ✅ null cũng tính là chưa đọc
                        hasUnread = true;
                        break;
                    }
                }
                setNotifyDotVisible(hasUnread);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };

        notifyRef.addValueEventListener(unreadListener);
    }

    private void stopNotifyBadgeListener() {
        if (notifyRef != null && unreadListener != null) {
            notifyRef.removeEventListener(unreadListener);
        }
        unreadListener = null;
        notifyRef = null;
    }

    private void setNotifyDotVisible(boolean visible) {
        if (viewNotifyDot == null) return;
        viewNotifyDot.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // ===== lifecycle =====
    @Override
    protected void onStart() {
        super.onStart();
        startNotifyBadgeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopNotifyBadgeListener();
    }
}
