package com.example.lostandfound;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
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

import java.util.Calendar;

public class PostActivity extends AppCompatActivity {

    // Khai báo biến
    private TextView tvPostTitle;
    private SwitchMaterial switchLoadImage;
    private ImageView imgPreview;
    private Button btnSelectImage, btnPickTime, btnSubmitPost;
    private EditText edtDescription, edtLocation, edtContact, edtTransactionPlace;
    private ImageButton btnMapMarker;

    private String postType; // "LOST" hoặc "FOUND"
    private Uri selectedImageUri;

    // Launcher để mở thư viện ảnh
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
        // 1. Nhận Mode từ HomeActivity
        postType = getIntent().getStringExtra("POST_TYPE");

        if ("LOST".equals(postType)) {
            tvPostTitle.setText("Đăng tin: TÌM ĐỒ BỊ MẤT");
            tvPostTitle.setTextColor(Color.parseColor("#D32F2F")); // Màu đỏ
            btnSubmitPost.setBackgroundColor(Color.parseColor("#D32F2F"));
            edtLocation.setHint("Bạn làm mất đồ ở đâu?");
        } else {
            tvPostTitle.setText("Đăng tin: NHẶT ĐƯỢC ĐỒ");
            tvPostTitle.setTextColor(Color.parseColor("#388E3C")); // Màu xanh lá
            btnSubmitPost.setBackgroundColor(Color.parseColor("#388E3C"));
            edtLocation.setHint("Bạn nhặt được đồ ở đâu?");
        }

        // 2. Xử lý Switch chọn Load ảnh hay không
        switchLoadImage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnSelectImage.setVisibility(View.VISIBLE);
                imgPreview.setVisibility(selectedImageUri != null ? View.VISIBLE : View.GONE);
            } else {
                btnSelectImage.setVisibility(View.GONE);
                imgPreview.setVisibility(View.GONE);
                selectedImageUri = null; // Xóa ảnh nếu tắt switch
            }
        });

        // 3. Sự kiện chọn ảnh
        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // 4. Sự kiện chọn Ngày/Giờ
        btnPickTime.setOnClickListener(v -> showDateTimePicker());

        // 5. Sự kiện nút Map (Giả lập)
        btnMapMarker.setOnClickListener(v -> {
            // TODO: Mở Activity Google Map hoặc Dialog bản đồ tại đây
            Toast.makeText(this, "Chức năng mở bản đồ để ghim vị trí", Toast.LENGTH_SHORT).show();
        });

        // 6. Sự kiện Submit
        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    // Sau khi chọn ngày thì chọn giờ
                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
                                String time = dayOfMonth + "/" + (month + 1) + "/" + year + " " + hourOfDay + ":" + minute;
                                btnPickTime.setText(time);
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void submitPost() {
        // Kiểm tra dữ liệu nhập
        if (edtDescription.getText().toString().isEmpty() || edtContact.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mô tả và thông tin liên hệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ở đây bạn sẽ viết code lưu dữ liệu vào Firebase hoặc Database
        String message = "Đang đăng bài: " + (postType.equals("LOST") ? "Mất đồ" : "Nhặt được");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        finish(); // Đóng Activity và quay về trang chủ
    }
}