package com.example.lostandfound;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Setting extends Fragment {

    private TextView tvUserEmail, tvUserPhone;
    private LinearLayout btnChangePassword, btnMyPosts, btnSupport;
    private MaterialButton btnLogout;
    private View cardAvatar;
    private ImageView imgAvatar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri == null) return;
                        try {
                            // preview
                            if (imgAvatar != null) {
                                imgAvatar.setImageURI(null);
                                imgAvatar.setPadding(0, 0, 0, 0);
                                imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                imgAvatar.setImageURI(uri);
                            }

                            android.graphics.Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);

                            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 400, 400, true);

                            String imageString = ImageUtil.bitmapToBase64(scaled);

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                DatabaseReference userRef = FirebaseDatabase.getInstance(
                                        "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app"
                                ).getReference("users").child(user.getUid());

                                userRef.child("avatarUrl").setValue(imageString)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Avatar saved", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(requireContext(), "User not signed in", Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "Image processing error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_setting, container, false);

        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);

        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnMyPosts = view.findViewById(R.id.btnMyPosts);
        btnSupport = view.findViewById(R.id.btnSupport);

        cardAvatar = view.findViewById(R.id.cardAvatar);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnLogout = view.findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();

        loadUserInfo();

        cardAvatar.setOnClickListener(v -> showChangeAvatarDialog());

        btnMyPosts.setOnClickListener(v -> openHistoryFragment());

        btnChangePassword.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                mAuth.sendPasswordResetEmail(mAuth.getCurrentUser().getEmail())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) Toast.makeText(requireContext(), "Password email sent", Toast.LENGTH_LONG).show();
                        });
            }
        });

        btnSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@lostandfound.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Lost & Found");
            startActivity(Intent.createChooser(intent, "Send support email"));
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void showChangeAvatarDialog() {
        String[] options = {"Chọn ảnh từ thư viện", "Xem ảnh đại diện"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ảnh đại diện");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    imagePickerLauncher.launch("image/*");
                } else {
                    if (imgAvatar != null && imgAvatar.getDrawable() != null) {
                        AlertDialog.Builder preview = new AlertDialog.Builder(requireContext());
                        ImageView iv = new ImageView(requireContext());
                        iv.setImageDrawable(imgAvatar.getDrawable());
                        iv.setAdjustViewBounds(true);
                        preview.setView(iv);
                        preview.setPositiveButton("Close", null);
                        preview.show();
                    } else {
                        Toast.makeText(requireContext(), "No avatar", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.show();
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        tvUserEmail.setText(user.getEmail());
        try {
            mDatabase = FirebaseDatabase.getInstance(
                    "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app"
            ).getReference("users").child(user.getUid());

            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) return;
                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone != null) tvUserPhone.setText(phone);

                    String base64Image = snapshot.child("avatarUrl").getValue(String.class);
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decoded = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            if (bmp != null && imgAvatar != null) {
                                imgAvatar.setImageBitmap(bmp);
                                imgAvatar.setPadding(0,0,0,0);
                                imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openHistoryFragment() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HistoryFragment())
                .addToBackStack("history")
                .commit();
    }

}
