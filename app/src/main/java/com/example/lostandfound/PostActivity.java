package com.example.lostandfound;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.Calendar;

public class PostActivity extends AppCompatActivity {

    // --- Khai báo biến ---
    private TextView tvPostTitle;
    private SwitchMaterial switchLoadImage;
    private ImageView imgPreview;
    private Button btnSelectImage, btnPickTime, btnSubmitPost;
    private EditText edtDescription, edtLocation, edtContact, edtTransactionPlace;
    private ImageButton btnMapMarker;

    private String postType; // "LOST" hoặc "FOUND"
    private Uri selectedImageUri;

    // Firebase (Bỏ Retrofit đi vì bạn chưa có Backend riêng)
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    // Launcher chọn ảnh
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imgPreview.setImageURI(uri);
                        imgPreview.setVisibility(View.VISIBLE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Khởi tạo Firebase
        databaseReference = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("posts");
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupLogic();
    }

    private void initViews() {
        tvPostTitle = findViewById(R.id.tvPostTitle);
        switchLoadImage = findViewById(R.id.switchLoadImage);
        imgPreview = findViewById(R.id.imgPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        edtDescription = findViewById(R.id.edtDescription);
        edtLocation = findViewById(R.id.edtLocation);
        btnMapMarker = findViewById(R.id.btnMapMarker);
        edtContact = findViewById(R.id.edtContact);
        edtTransactionPlace = findViewById(R.id.edtTransactionPlace);
    }

    private void setupLogic() {
        postType = getIntent().getStringExtra("POST_TYPE");
        if (postType == null) postType = "LOST"; // Mặc định

        // Đổi màu giao diện theo loại tin
        if ("LOST".equals(postType)) {
            tvPostTitle.setText("Đăng tin: TÌM ĐỒ BỊ MẤT");
            tvPostTitle.setTextColor(Color.parseColor("#D32F2F"));
            btnSubmitPost.setBackgroundColor(Color.parseColor("#D32F2F"));
            edtLocation.setHint("Bạn làm mất đồ ở đâu?");
        } else {
            tvPostTitle.setText("Đăng tin: NHẶT ĐƯỢC ĐỒ");
            tvPostTitle.setTextColor(Color.parseColor("#388E3C"));
            btnSubmitPost.setBackgroundColor(Color.parseColor("#388E3C"));
            edtLocation.setHint("Bạn nhặt được đồ ở đâu?");
        }

        // Xử lý bật/tắt chọn ảnh
        switchLoadImage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnSelectImage.setVisibility(View.VISIBLE);
                if (selectedImageUri != null) imgPreview.setVisibility(View.VISIBLE);
            } else {
                btnSelectImage.setVisibility(View.GONE);
                imgPreview.setVisibility(View.GONE);
                selectedImageUri = null;
            }
        });

        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnPickTime.setOnClickListener(v -> showDateTimePicker());
        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                String strMinute = (minute < 10) ? "0" + minute : String.valueOf(minute);
                String strHour = (hourOfDay < 10) ? "0" + hourOfDay : String.valueOf(hourOfDay);
                String time = dayOfMonth + "/" + (month + 1) + "/" + year + " " + strHour + ":" + strMinute;
                btnPickTime.setText(time);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submitPost() {
        // 1. Validate
        String description = edtDescription.getText().toString().trim();
        String contact = edtContact.getText().toString().trim();
        String address = edtLocation.getText().toString().trim();
        String transactionPlace = edtTransactionPlace.getText().toString().trim();
        String timePosted = btnPickTime.getText().toString();

        if (description.isEmpty() || contact.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!transactionPlace.isEmpty()) description += "\n(GD tại: " + transactionPlace + ")";

        btnSubmitPost.setEnabled(false);
        btnSubmitPost.setText("Đang gửi...");

        // 2. Xử lý ảnh Base64
        String imageBase64 = "";
        if (selectedImageUri != null && switchLoadImage.isChecked()) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                // Giảm kích thước ảnh trước khi convert để tránh lỗi Firebase quá nặng
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
                imageBase64 = ImageUtil.bitmapToBase64(scaledBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 3. Chuẩn bị data
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = (currentUser != null) ? currentUser.getEmail() : "Ẩn danh";
        String postId = databaseReference.push().getKey();

        // **LƯU Ý QUAN TRỌNG**: Bạn phải chắc chắn file Post.java của bạn có Constructor khớp với dòng này
        Post newPost = new Post(postId, userEmail, timePosted, description, postType, imageBase64, contact, address);

        // 4. Đẩy lên Firebase
        if (postId != null) {
            databaseReference.child(postId).setValue(newPost)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(PostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                            finish(); // Đóng màn hình này (Tự động "Lau đi")
                        } else {
                            Toast.makeText(PostActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSubmitPost.setEnabled(true);
                            btnSubmitPost.setText("ĐĂNG BÀI");
                        }
                    });
        }
    }
}