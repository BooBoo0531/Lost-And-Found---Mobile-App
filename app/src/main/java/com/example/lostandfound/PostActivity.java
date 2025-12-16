package com.example.lostandfound;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.Calendar;

public class PostActivity extends AppCompatActivity {

    private MaterialToolbar toolbarPost;
    private SwitchMaterial switchLoadImage;
    private ImageView imgPreview;
    private Button btnSelectImage, btnSubmitPost;

    private TextInputEditText edtDescription, edtLocation, edtContact, edtTransactionPlace;
    private TextInputEditText edtPickTime;
    private TextInputLayout tilLocation;

    private String postType;
    private Uri selectedImageUri;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<String> imagePickerLauncher;

    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    selectedImageUri = uri;

                    if (imgPreview == null) return;

                    // 🔥 XÓA HOÀN TOÀN ICON + TINT
                    imgPreview.setImageDrawable(null);
                    imgPreview.setImageTintList(null); // QUAN TRỌNG
                    imgPreview.setColorFilter(null);

                    imgPreview.setBackground(null);
                    imgPreview.setPadding(0, 0, 0, 0);
                    imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    imgPreview.setImageURI(uri);
                    imgPreview.setVisibility(View.VISIBLE);
                }
        );
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        try {
            databaseReference = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("posts");
        } catch (Exception e) {
            databaseReference = null;
            Toast.makeText(this, "Lỗi Database URL", Toast.LENGTH_SHORT).show();
        }
        mAuth = FirebaseAuth.getInstance();

        initViews();
        registerImagePicker();
        setupLogic();
    }

    private void initViews() {
        toolbarPost = findViewById(R.id.toolbarPost);

        switchLoadImage = findViewById(R.id.switchLoadImage);
        imgPreview = findViewById(R.id.imgPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        edtDescription = findViewById(R.id.edtDescription);
        edtLocation = findViewById(R.id.edtLocation);
        tilLocation = findViewById(R.id.tilLocation);
        edtPickTime = findViewById(R.id.btnPickTime);
        edtContact = findViewById(R.id.edtContact);
        edtTransactionPlace = findViewById(R.id.edtTransactionPlace);

        // Ensure a clear default text so validation works reliably
        if (edtPickTime.getText() == null || edtPickTime.getText().toString().trim().isEmpty()) {
            edtPickTime.setText("Chọn thời gian");
        }

        btnSubmitPost = findViewById(R.id.btnSubmitPost);
    }

    private void setupLogic() {
        postType = getIntent().getStringExtra("POST_TYPE");
        if (postType == null) postType = "LOST";

        configureUIByType();

        if (toolbarPost != null) {
            toolbarPost.setNavigationOnClickListener(v -> finish());
        }

        btnSelectImage.setOnClickListener(v -> {
            if (imagePickerLauncher != null) {
                imagePickerLauncher.launch("image/*");
            } else {
                Toast.makeText(PostActivity.this, "Image picker not ready", Toast.LENGTH_SHORT).show();
            }
        });

        edtPickTime.setOnClickListener(v -> showDateTimePicker());

        if (tilLocation != null) {
            tilLocation.setEndIconOnClickListener(v ->
                    Toast.makeText(this, "Chức năng chọn bản đồ đang phát triển!", Toast.LENGTH_SHORT).show()
            );
        }

        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void configureUIByType() {
        if (toolbarPost == null) return;

        String locationHintString = "";
        String descHintString = "";

        // 1. Xác định nội dung Hint dựa trên loại tin (LOST/FOUND)
        if ("LOST".equals(postType)) {
            toolbarPost.setTitle("Đăng tin: TÌM ĐỒ BỊ MẤT");
            toolbarPost.setBackgroundColor(Color.parseColor("#F07A7A"));
            btnSubmitPost.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F07A7A")));

            locationHintString = "Bạn làm mất đồ ở đâu?";
            descHintString = "Mô tả chi tiết đồ vật bị mất...";
        } else {
            toolbarPost.setTitle("Đăng tin: NHẶT ĐƯỢC ĐỒ");
            toolbarPost.setBackgroundColor(Color.parseColor("#388E3C"));
            btnSubmitPost.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#388E3C")));

            locationHintString = "Bạn nhặt được đồ ở đâu?";
            descHintString = "Mô tả chi tiết đồ vật nhặt được...";
        }

        setupDynamicHint(edtLocation, locationHintString);
        setupDynamicHint(edtDescription, descHintString);
    }

    // Tự động ẩn/hiện Hint khi bấm vào ---
    private void setupDynamicHint(TextInputEditText editText, String hintText) {
        editText.setHint("");

        // Bắt sự kiện khi người dùng bấm vào ô nhập
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Khi bấm vào -> Hiện Hint lên
                editText.setHint(hintText);
            } else {
                // Khi bấm ra ngoài -> Lại xóa Hint đi
                editText.setHint("");
            }
        });
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                String strMinute = (minute < 10) ? "0" + minute : String.valueOf(minute);
                String strHour = (hourOfDay < 10) ? "0" + hourOfDay : String.valueOf(hourOfDay);
                String time = dayOfMonth + "/" + (month + 1) + "/" + year + " " + strHour + ":" + strMinute;
                edtPickTime.setText(time);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submitPost() {
        String description = edtDescription.getText() != null ? edtDescription.getText().toString().trim() : "";
        String contact = edtContact.getText() != null ? edtContact.getText().toString().trim() : "";
        String address = edtLocation.getText() != null ? edtLocation.getText().toString().trim() : "";
        String transactionPlace = edtTransactionPlace.getText() != null ? edtTransactionPlace.getText().toString().trim() : "";
        String timePosted = edtPickTime.getText() != null ? edtPickTime.getText().toString() : "";

        if (description.isEmpty()) {
            edtDescription.setError("Vui lòng nhập mô tả!");
            edtDescription.requestFocus();
            return;
        }
        if (address.isEmpty()) {
            edtLocation.setError("Vui lòng nhập địa điểm!");
            edtLocation.requestFocus();
            return;
        }
        if (timePosted.equals("Chọn thời gian") || timePosted.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn thời gian xảy ra!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (contact.isEmpty()) {
            edtContact.setError("Vui lòng nhập thông tin liên hệ!");
            edtContact.requestFocus();
            return;
        }

        if (!transactionPlace.isEmpty()) {
            description += "\n(Giao dịch tại: " + transactionPlace + ")";
        }

        btnSubmitPost.setEnabled(false);
        btnSubmitPost.setText("Đang gửi...");

        String imageBase64 = "";
        if (selectedImageUri != null) {
            try {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.Source src = ImageDecoder.createSource(getContentResolver(), selectedImageUri);
                    bitmap = ImageDecoder.decodeBitmap(src);
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                }

                int maxSize = 1024;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                float bitmapRatio = (float) width / (float) height;
                if (bitmapRatio > 1) {
                    width = maxSize;
                    height = (int) (width / bitmapRatio);
                } else {
                    height = maxSize;
                    width = (int) (height * bitmapRatio);
                }

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

                // ImageUtil must exist in project
                imageBase64 = ImageUtil.bitmapToBase64(scaledBitmap);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
                // Allow submission without image or abort; here we continue without image
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi không xác định khi xử lý ảnh", Toast.LENGTH_SHORT).show();
            }
        }

        if (databaseReference == null) {
            Toast.makeText(this, "Database chưa cấu hình. Không thể gửi bài.", Toast.LENGTH_SHORT).show();
            btnSubmitPost.setEnabled(true);
            btnSubmitPost.setText("ĐĂNG BÀI NGAY");
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = (currentUser != null) ? currentUser.getEmail() : "Ẩn danh";
        String userId = (currentUser != null) ? currentUser.getUid() : "";
        String postId = databaseReference.push().getKey();

        if (postId == null) {
            Toast.makeText(this, "Lỗi tạo ID bài viết", Toast.LENGTH_SHORT).show();
            btnSubmitPost.setEnabled(true);
            btnSubmitPost.setText("ĐĂNG BÀI NGAY");
            return;
        }

        Post newPost = new Post(postId,userId, userEmail, timePosted, description, postType, imageBase64, contact, address);

        databaseReference.child(postId).setValue(newPost)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(PostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PostActivity.this, "Lỗi: " + (task.getException() != null ? task.getException().getMessage() : "Không rõ"), Toast.LENGTH_SHORT).show();
                        btnSubmitPost.setEnabled(true);
                        btnSubmitPost.setText("ĐĂNG BÀI NGAY");
                    }
                });
    }
}
