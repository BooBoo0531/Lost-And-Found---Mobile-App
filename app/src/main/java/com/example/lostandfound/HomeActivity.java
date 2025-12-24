package com.example.lostandfound;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.io.IOException;

public class HomeActivity extends AppCompatActivity {

    // ===== UI =====
    private FloatingActionButton fabCreatePost, fabHome, fabLost, fabFound, fabOpenChatList;
    private TextView tvLostLabel, tvFoundLabel;
    private boolean isFabExpanded = false;

    private LinearLayout btnNavHistory, btnNavMap, btnNavNotify, btnNavSetting;
    private View viewNotifyDot;

    private AppBarLayout appBarLayout;
    private View btnSearchSmall;
    private View fragmentContainer;

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

    private EditText edtSearchHome;
    private ImageView btnClearSearch;

    private ImageView btnCameraSearch;
    private ActivityResultLauncher<String> searchImageLauncher;

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

        // Ánh xạ View
        appBarLayout = findViewById(R.id.appBarLayout);
        btnSearchSmall = findViewById(R.id.btnSearchSmall);
        fragmentContainer = findViewById(R.id.fragment_container);
        edtSearchHome = findViewById(R.id.edtSearchHome);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnCameraSearch = findViewById(R.id.btnCameraSearch);
        fabOpenChatList = findViewById(R.id.fabOpenChatList);

        // Insets (notch)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fabOpenChatList.setOnClickListener(v -> {
            // Kiểm tra đăng nhập
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(HomeActivity.this, "Bạn cần đăng nhập!", Toast.LENGTH_SHORT).show();
            } else {
                // Mở màn hình danh sách chat
                Intent intent = new Intent(HomeActivity.this, ChatListActivity.class);
                startActivity(intent);
            }
        });

        setupSearchLogic();
        setupCollapsingHeader();
        initFabMenu();
        initBottomNavigation();
        registerSearchImagePicker();

        if (btnCameraSearch != null) {
            btnCameraSearch.setOnClickListener(v -> {
                searchImageLauncher.launch("image/*"); // Mở thư viện ảnh
            });
        }

        if (savedInstanceState == null) {
            loadFragment(new NewsFeedFragment());
            updateHeaderVisibility(true);
        }
    }

    private void registerSearchImagePicker() {
        searchImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        analyzeImageForSearch(uri);
                    }
                }
        );
    }

    // Hàm phân tích ảnh bằng AI
    private void analyzeImageForSearch(android.net.Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);

            // Tạo bộ phân tích (Labeler)
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            labeler.process(image)
                    .addOnSuccessListener(labels -> {
                        if (!labels.isEmpty()) {
                            String englishKeyword = labels.get(0).getText();

                            // GỌI HÀM DỊCH THỦ CÔNG CỦA BẠN
                            manualTranslateAndSearch(englishKeyword);

                        } else {
                            Toast.makeText(this, "Không nhận diện được", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> { /* ... */ });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manualTranslateAndSearch(String englishKeyword) {
        String vietnameseKeyword = englishKeyword;

        // Tự định nghĩa các từ hay gặp
        switch (englishKeyword.toLowerCase()) {
            case "wallet": vietnameseKeyword = "ví"; break;
            case "purse": vietnameseKeyword = "túi xách"; break;
            case "cell phone":
            case "mobile phone": vietnameseKeyword = "điện thoại"; break;
            case "computer":
            case "laptop": vietnameseKeyword = "laptop"; break;
            case "key": vietnameseKeyword = "chìa khóa"; break;
            case "cat": vietnameseKeyword = "mèo"; break;
            case "dog": vietnameseKeyword = "chó"; break;
            case "backpack": vietnameseKeyword = "balo"; break;
            // ... thêm các từ khác tùy ý
        }

        if (edtSearchHome != null) {
            edtSearchHome.setText(vietnameseKeyword);
        }
    }

    private void setupSearchLogic() {
        // Xử lý khi gõ chữ
        if (edtSearchHome != null) {
            edtSearchHome.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String keyword = s.toString();
                    postVM.search(keyword); // Gọi ViewModel

                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(keyword.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Xử lý nút Xóa
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                edtSearchHome.setText("");
                postVM.search("");
            });
        }

        // Xử lý nút Search nhỏ (khi đang thu hẹp)
        if (btnSearchSmall != null) {
            btnSearchSmall.setOnClickListener(v -> {
                // 1. Mở rộng AppBar ra
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, true);
                }
                // 2. Focus vào ô nhập liệu
                if (edtSearchHome != null) {
                    edtSearchHome.requestFocus();
                    // Hiện bàn phím (Optional)
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(edtSearchHome, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    // --- XỬ LÝ ẨN/HIỆN HEADER ---
    private void updateHeaderVisibility(boolean isVisible) {
        if (appBarLayout == null || fragmentContainer == null) return;

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) fragmentContainer.getLayoutParams();

        if (isVisible) {
            if (appBarLayout.getVisibility() != View.VISIBLE) {
                appBarLayout.setVisibility(View.VISIBLE);

                params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
                fragmentContainer.setLayoutParams(params);
            }
            appBarLayout.setExpanded(true, true);
        } else {
            if (appBarLayout.getVisibility() != View.GONE) {
                appBarLayout.setVisibility(View.GONE);

                params.setBehavior(null);
                fragmentContainer.setLayoutParams(params);
            }
        }
    }

    private void setupCollapsingHeader() {
        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
                float percentage = (float) Math.abs(verticalOffset) / appBar.getTotalScrollRange();

                // Khi thu hẹp > 75% -> Hiện nút search nhỏ
                if (percentage > 0.75f) {
                    if (btnSearchSmall != null && btnSearchSmall.getVisibility() != View.VISIBLE) {
                        btnSearchSmall.setVisibility(View.VISIBLE);
                        btnSearchSmall.animate().alpha(1f).setDuration(200).start();
                    }
                }
                // Khi mở rộng -> Ẩn nút search nhỏ
                else {
                    if (btnSearchSmall != null && btnSearchSmall.getVisibility() == View.VISIBLE) {
                        btnSearchSmall.setVisibility(View.INVISIBLE); // Dùng Invisible để không vỡ layout
                        btnSearchSmall.setAlpha(0f);
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

        // Nút Map -> Ẩn Header
        if (btnNavMap != null) {
            btnNavMap.setOnClickListener(v -> {
                loadFragment(new MapFragment());
                updateHeaderVisibility(false);
            });
        }

        // Nút Setting -> Ẩn Header
        if (btnNavSetting != null) {
            btnNavSetting.setOnClickListener(v -> {
                loadFragment(new Setting());
                updateHeaderVisibility(false);
            });
        }

        // Nút History -> Ẩn Header
        if (btnNavHistory != null) {
            btnNavHistory.setOnClickListener(v -> {
                loadFragment(new HistoryFragment());
                updateHeaderVisibility(false);
            });
        }

        // Nút Notify -> Ẩn Header
        if (btnNavNotify != null) {
            btnNavNotify.setOnClickListener(v -> {
                loadFragment(new NotificationFragment());
                updateHeaderVisibility(false);
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

        // Nút Home -> HIỆN Header
        if (fabHome != null) {
            fabHome.setOnClickListener(v -> {
                if (isFabExpanded) closeFabMenu();
                loadFragment(new NewsFeedFragment());
                updateHeaderVisibility(true); // Hiện lại Header
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
                    if (isRead == null || !isRead) {
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