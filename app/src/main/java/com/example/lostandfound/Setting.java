package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Setting extends Fragment {

    private TextView tvUserEmail, tvUserPhone;
    private LinearLayout btnLogout, btnChangePassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_setting, container, false);

        // Ánh xạ
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);

        mAuth = FirebaseAuth.getInstance();

        // Load thông tin user
        loadUserInfo();

        // Xử lý Đăng Xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // Đăng xuất khỏi Firebase

            // Chuyển về màn hình Đăng nhập và xóa sạch lịch sử Home
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        // Xử lý Đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng đang phát triển", Toast.LENGTH_SHORT).show();
            // Hoặc điều hướng sang màn hình ChangePasswordActivity nếu bạn làm thêm
        });

        return view;
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 1. Hiển thị Email từ Auth
            tvUserEmail.setText(user.getEmail());

            // 2. Lấy số điện thoại từ Realtime Database (Nếu có lưu)
            String dbUrl = "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";
            mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(user.getUid());

            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Lấy về object User (class User bạn đã tạo lúc đăng ký)
                        User userProfile = snapshot.getValue(User.class);
                        if (userProfile != null) {
                            tvUserPhone.setText(userProfile.phone);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Không làm gì hoặc log lỗi
                }
            });
        }

    }
}