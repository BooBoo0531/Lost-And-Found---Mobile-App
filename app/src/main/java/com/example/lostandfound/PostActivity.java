package com.example.lostandfound;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class PostActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "MODE";
    public static final String MODE_EDIT = "EDIT";
    public static final String EXTRA_POST = "EXTRA_POST";

    private MaterialToolbar toolbarPost;
    private SwitchMaterial switchLoadImage;
    private ImageView imgPreview;
    private MaterialButton btnSelectImage, btnSubmitPost;

    private TextInputEditText edtDescription, edtLocation, edtContact, edtPickTime;
    private TextInputEditText edtTransactionPlace; // giữ biến để không crash nếu XML còn, nhưng sẽ ẩn
    private TextInputLayout tilLocation, tilDescription, tilPickTime, tilContact, tilTransactionPlace;

    private String postType;
    private Uri selectedImageUri;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private boolean isEditMode = false;
    private String editingPostId = "";
    private String existingImageBase64 = "";
    private Post editingPost;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    private double pickedLat = 0;
    private double pickedLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        try {
            databaseReference = FirebaseDatabase
                    .getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("posts");
        } catch (Exception e) {
            databaseReference = null;
            Toast.makeText(this, "Lỗi kết nối Database", Toast.LENGTH_SHORT).show();
        }

        mAuth = FirebaseAuth.getInstance();

        initViews();
        registerImagePicker();
        registerLocationPicker();
        setupLogic();

        handleEditModeIfAny();
    }

    private void initViews() {
        toolbarPost = findViewById(R.id.toolbarPost);
        switchLoadImage = findViewById(R.id.switchLoadImage);
        imgPreview = findViewById(R.id.imgPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);

        edtDescription = findViewById(R.id.edtDescription);
        edtLocation = findViewById(R.id.edtLocation);

        tilLocation = findViewById(R.id.tilLocation);
        tilDescription = findViewById(R.id.tilDescription);
        tilPickTime = findViewById(R.id.tilPickTime);
        tilContact = findViewById(R.id.tilContact);
        tilTransactionPlace = findViewById(R.id.tilTransactionPlace);

        edtPickTime = findViewById(R.id.btnPickTime);
        edtContact = findViewById(R.id.edtContact);
        edtTransactionPlace = findViewById(R.id.edtTransactionPlace);

        // ✅ Ẩn ô "Giao dịch tại" (không dùng nữa)
        if (tilTransactionPlace != null) {
            tilTransactionPlace.setVisibility(View.GONE);
        }

        if (edtPickTime != null && (edtPickTime.getText() == null || edtPickTime.getText().toString().trim().isEmpty())) {
            edtPickTime.setText("Chọn thời gian");
        }

        // (Tuỳ chọn) đổi hint label cho đúng ngữ nghĩa
        if (tilPickTime != null) tilPickTime.setHint("Thời gian nhặt/mất");
    }

    private void setupLogic() {
        postType = getIntent().getStringExtra("POST_TYPE");
        if (postType == null) postType = "LOST";

        configureUIByType();

        if (toolbarPost != null) toolbarPost.setNavigationOnClickListener(v -> finish());

        if (btnSelectImage != null) {
            btnSelectImage.setOnClickListener(v -> {
                if (imagePickerLauncher != null) imagePickerLauncher.launch("image/*");
            });
        }

        if (edtPickTime != null) {
            edtPickTime.setOnClickListener(v -> showDateTimePicker());
        }

        if (tilLocation != null) {
            tilLocation.setEndIconOnClickListener(v -> {
                Intent i = new Intent(PostActivity.this, PickLocationActivity.class);
                if (locationPickerLauncher != null) locationPickerLauncher.launch(i);
                else Toast.makeText(this, "Chưa sẵn sàng chọn vị trí", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSubmitPost != null) {
            btnSubmitPost.setOnClickListener(v -> submitPost());
        }
    }

    private void configureUIByType() {
        if (toolbarPost == null || btnSubmitPost == null) return;

        int themeColor;
        String locationHintString;
        String descHintString;

        if ("LOST".equalsIgnoreCase(postType)) {
            toolbarPost.setTitle("Đăng tin: TÌM ĐỒ BỊ MẤT");
            themeColor = Color.parseColor("#F07A7A");
            locationHintString = "Bạn làm mất đồ ở đâu?";
            descHintString = "Mô tả chi tiết đồ vật bị mất...";
        } else {
            toolbarPost.setTitle("Đăng tin: NHẶT ĐƯỢC ĐỒ");
            themeColor = Color.parseColor("#59AC77");
            locationHintString = "Bạn nhặt được đồ ở đâu?";
            descHintString = "Mô tả chi tiết đồ vật nhặt được...";
        }

        toolbarPost.setBackgroundColor(themeColor);
        btnSubmitPost.setBackgroundTintList(ColorStateList.valueOf(themeColor));
        btnSubmitPost.setTextColor(Color.WHITE);

        if (btnSelectImage != null) {
            btnSelectImage.setTextColor(themeColor);
            btnSelectImage.setIconTint(ColorStateList.valueOf(themeColor));
            int paleColor = ("LOST".equalsIgnoreCase(postType))
                    ? Color.parseColor("#FFF0F0")
                    : Color.parseColor("#E8F5E9");
            btnSelectImage.setBackgroundTintList(ColorStateList.valueOf(paleColor));
        }

        applyColorToLayout(tilLocation, themeColor);
        applyColorToLayout(tilDescription, themeColor);
        applyColorToLayout(tilPickTime, themeColor);
        applyColorToLayout(tilContact, themeColor);

        if (tilLocation != null) tilLocation.setEndIconTintList(ColorStateList.valueOf(themeColor));

        if (tilLocation != null) tilLocation.setHint(locationHintString);
        else if (edtLocation != null) edtLocation.setHint(locationHintString);

        if (tilDescription != null) tilDescription.setHint(descHintString);
        else if (edtDescription != null) edtDescription.setHint(descHintString);
    }

    private void applyColorToLayout(TextInputLayout til, int color) {
        if (til != null) til.setBoxStrokeColor(color);
    }

    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                if (imgPreview != null) {
                    imgPreview.setImageURI(uri);
                    imgPreview.setVisibility(View.VISIBLE);
                    imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
        });
    }

    private void registerLocationPicker() {
        locationPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String address = result.getData().getStringExtra(PickLocationActivity.EXTRA_ADDRESS);
                pickedLat = result.getData().getDoubleExtra(PickLocationActivity.EXTRA_LAT, 0);
                pickedLng = result.getData().getDoubleExtra(PickLocationActivity.EXTRA_LNG, 0);

                if (address != null && !address.trim().isEmpty()) {
                    if (edtLocation != null) edtLocation.setText(address);
                } else {
                    if (edtLocation != null) edtLocation.setText(pickedLat + ", " + pickedLng);
                }
                if (edtLocation != null) edtLocation.setError(null);
            }
        });
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                    String time = dayOfMonth + "/" + (month + 1) + "/" + year + " "
                            + ((hourOfDay < 10) ? "0" + hourOfDay : hourOfDay)
                            + ":" + ((minute < 10) ? "0" + minute : minute);
                    if (edtPickTime != null) edtPickTime.setText(time);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void handleEditModeIfAny() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        isEditMode = MODE_EDIT.equalsIgnoreCase(mode);
        if (!isEditMode) return;

        editingPost = readPostFromIntent();
        if (editingPost == null) { finish(); return; }

        editingPostId = safe(editingPost.getId());
        postType = safe(editingPost.getPostType());
        if (postType.isEmpty()) postType = "LOST";

        configureUIByType();
        fillFormForEdit(editingPost);

        if (toolbarPost != null) toolbarPost.setTitle("Chỉnh sửa bài viết");
        if (btnSubmitPost != null) btnSubmitPost.setText("LƯU THAY ĐỔI");
    }

    private Post readPostFromIntent() {
        try {
            if (Build.VERSION.SDK_INT >= 33)
                return getIntent().getSerializableExtra(EXTRA_POST, Post.class);
            else
                return (Post) getIntent().getSerializableExtra(EXTRA_POST);
        } catch (Exception e) {
            return null;
        }
    }

    private void fillFormForEdit(@NonNull Post post) {
        if (edtDescription != null) {
            edtDescription.setText(stripTransaction(post.getDescription()));
        }
        if (edtLocation != null) edtLocation.setText(safe(post.getAddress()));
        if (edtContact != null) edtContact.setText(safe(post.getContact()));

        // ✅ Edit: hiển thị thời gian nhặt/mất (user cung cấp)
        String lf = safe(post.getLostFoundTime());
        if (lf.isEmpty()) {
            // fallback bài cũ: timePosted từng là nhặt/mất
            lf = safe(post.getTimePosted());
        }
        if (edtPickTime != null) edtPickTime.setText(lf.isEmpty() ? "Chọn thời gian" : lf);

        pickedLat = post.getLat();
        pickedLng = post.getLng();

        existingImageBase64 = safe(post.getImageBase64());
        if (!existingImageBase64.isEmpty()) {
            Bitmap bmp = ImageUtil.base64ToBitmap(existingImageBase64);
            if (bmp != null && imgPreview != null) {
                imgPreview.setVisibility(View.VISIBLE);
                imgPreview.setImageBitmap(bmp);
            }
        }
    }

    // ✅ format "thời gian đăng" lúc bấm đăng/lưu
    private String nowFormatted() {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1;
        int year = c.get(Calendar.YEAR);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);

        String hh = (hour < 10) ? "0" + hour : String.valueOf(hour);
        String mm = (min < 10) ? "0" + min : String.valueOf(min);

        return day + "/" + month + "/" + year + " " + hh + ":" + mm;
    }

    // ✅ dọn transaction place trong bài cũ (nếu còn)
    private String stripTransaction(String desc) {
        if (desc == null) return "";
        return desc.replaceAll("\\n?\\(\\s*Giao\\s*dịch\\s*tại\\s*:\\s*[^\\)]*\\)", "").trim();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void submitPost() {
        String description = (edtDescription != null && edtDescription.getText() != null)
                ? edtDescription.getText().toString().trim()
                : "";
        String contact = (edtContact != null && edtContact.getText() != null)
                ? edtContact.getText().toString().trim()
                : "";
        String address = (edtLocation != null && edtLocation.getText() != null)
                ? edtLocation.getText().toString().trim()
                : "";

        // ✅ thời gian nhặt/mất do user chọn
        String lostFoundTime = (edtPickTime != null && edtPickTime.getText() != null)
                ? edtPickTime.getText().toString()
                : "";

        // ✅ thời gian đăng bài auto
        String timePosted = nowFormatted();

        description = stripTransaction(description);

        if (description.isEmpty()) {
            if (edtDescription != null) {
                edtDescription.setError("Vui lòng nhập mô tả!");
                edtDescription.requestFocus();
            }
            return;
        }

        if (address.isEmpty()) {
            if (edtLocation != null) {
                edtLocation.setError("Vui lòng nhập địa điểm!");
                edtLocation.requestFocus();
            }
            return;
        }

        if ("Chọn thời gian".equalsIgnoreCase(lostFoundTime) || lostFoundTime.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn thời gian nhặt/mất!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (contact.isEmpty()) {
            if (edtContact != null) {
                edtContact.setError("Vui lòng nhập thông tin liên hệ!");
                edtContact.requestFocus();
            }
            return;
        }

        if (btnSubmitPost != null) {
            btnSubmitPost.setEnabled(false);
            btnSubmitPost.setText(isEditMode ? "Đang lưu..." : "Đang gửi...");
        }

        String imageBase64 = isEditMode ? existingImageBase64 : "";
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
                float ratio = (float) width / (float) height;

                if (ratio > 1) {
                    width = maxSize;
                    height = (int) (width / ratio);
                } else {
                    height = maxSize;
                    width = (int) (height * ratio);
                }

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                imageBase64 = ImageUtil.bitmapToBase64(scaledBitmap);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            }
        }

        if (databaseReference == null) {
            Toast.makeText(this, "Lỗi kết nối Database", Toast.LENGTH_SHORT).show();
            if (btnSubmitPost != null) btnSubmitPost.setEnabled(true);
            return;
        }

        if (isEditMode) {
            updateExistingPost(description, timePosted, lostFoundTime, imageBase64, contact, address);
        } else {
            createNewPost(description, timePosted, lostFoundTime, imageBase64, contact, address);
        }
    }

    private void createNewPost(String description, String timePosted, String lostFoundTime,
                               String imageBase64, String contact, String address) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = (currentUser != null) ? currentUser.getEmail() : "Ẩn danh";
        String userId = (currentUser != null) ? currentUser.getUid() : "";
        String postId = databaseReference.push().getKey();
        if (postId == null) return;

        Post newPost = new Post(
                postId, userId, userEmail,
                timePosted, lostFoundTime,
                description, postType, imageBase64, contact, address
        );

        newPost.setLat(pickedLat);
        newPost.setLng(pickedLng);

        databaseReference.child(postId).setValue(newPost).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(PostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                Intent data = new Intent();
                data.putExtra("NEW_POST", newPost);
                setResult(RESULT_OK, data);
                finish();
            } else {
                Toast.makeText(PostActivity.this, "Lỗi đăng bài", Toast.LENGTH_SHORT).show();
                if (btnSubmitPost != null) {
                    btnSubmitPost.setEnabled(true);
                    btnSubmitPost.setText("ĐĂNG BÀI NGAY");
                }
            }
        });
    }

    private void updateExistingPost(String description, String timePosted, String lostFoundTime,
                                    String imageBase64, String contact, String address) {
        String userId = safe(editingPost.getUserId());
        String userEmail = safe(editingPost.getUserEmail());

        Post updated = new Post(
                editingPostId, userId, userEmail,
                timePosted, lostFoundTime,
                description, postType, imageBase64, contact, address
        );

        updated.setLat(pickedLat);
        updated.setLng(pickedLng);

        databaseReference.child(editingPostId).setValue(updated).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(PostActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(PostActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                if (btnSubmitPost != null) {
                    btnSubmitPost.setEnabled(true);
                    btnSubmitPost.setText("LƯU THAY ĐỔI");
                }
            }
        });
    }
}