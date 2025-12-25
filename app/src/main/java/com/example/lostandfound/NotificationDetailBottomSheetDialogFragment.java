package com.example.lostandfound;

import android.content.Intent; // Cần import Intent
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationDetailBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_NOTIFY = "ARG_NOTIFY";
    private static final String DB_URL =
            "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static NotificationDetailBottomSheetDialogFragment newInstance(@NonNull NotificationItem item) {
        NotificationDetailBottomSheetDialogFragment f = new NotificationDetailBottomSheetDialogFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_NOTIFY, item);
        f.setArguments(b);
        return f;
    }

    private NotificationItem item;

    private LinearLayout rootLayout; // Layout gốc để bắt sự kiện click
    private ImageView imgAvatar;
    private TextView tvName, tvType, tvTime, tvMessage;
    // private Button btnOpenComments; // Đã xóa

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_notification_detail, container, false);

        Object raw = getArguments() != null ? getArguments().getSerializable(ARG_NOTIFY) : null;
        if (raw instanceof NotificationItem) item = (NotificationItem) raw;

        rootLayout = v.findViewById(R.id.rootLayout); // Ánh xạ layout gốc
        imgAvatar = v.findViewById(R.id.imgDetailAvatar);
        tvName = v.findViewById(R.id.tvDetailName);
        tvType = v.findViewById(R.id.tvDetailType);
        tvTime = v.findViewById(R.id.tvDetailTime);
        tvMessage = v.findViewById(R.id.tvDetailMessage);

        bindData();
        return v;
    }

    private void bindData() {
        if (item == null) return;

        String who = (item.fromEmail == null || item.fromEmail.trim().isEmpty()) ? "Ai đó" : item.fromEmail;
        tvName.setText(who);

        String typeText = "COMMENT".equalsIgnoreCase(item.type)
                ? "Bình luận"
                : ("REPLY".equalsIgnoreCase(item.type) ? "Trả lời" : "Thông báo");
        tvType.setText(typeText);

        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(item.timestamp));
        tvTime.setText(time);

        tvMessage.setText(item.content == null ? "" : item.content);

        // Reset avatar mặc định
        imgAvatar.setImageResource(R.drawable.ic_notification);
        imgAvatar.setPadding(dp(6), dp(6), dp(6), dp(6));
        imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAvatar(item.fromUserId);

        // --- XỬ LÝ CLICK VÀO TOÀN BỘ THÔNG BÁO ---
        rootLayout.setOnClickListener(v -> {
            if (item.postId != null && !item.postId.trim().isEmpty()) {

                // CÁCH 1: Mở màn hình chi tiết bài đăng (Activity) - KHUYÊN DÙNG
                // Bạn cần thay PostDetailActivity.class bằng tên Activity bài đăng thực tế của bạn
                try {
                    Intent intent = new Intent(requireContext(), NewsFeedFragment.class);
                    intent.putExtra("POST_ID", item.postId);
                    startActivity(intent);
                    dismiss(); // Đóng thông báo sau khi nhấn
                } catch (Exception e) {
                    // Nếu chưa có Activity, hoặc lỗi, fallback về hiện Comment như cũ:
                    openCommentsSheet();
                }

                // CÁCH 2: Nếu bạn chỉ muốn hiện lại cái BottomSheet bình luận như cũ:
                // openCommentsSheet();
            }
        });
    }

    // Hàm phụ trợ mở lại BottomSheet Comments (Logic cũ của nút bấm)
    private void openCommentsSheet() {
        try {
            CommentsBottomSheetDialogFragment.newInstance(item.postId)
                    .show(requireActivity().getSupportFragmentManager(), "COMMENTS_" + item.postId);
            dismiss();
        } catch (Exception ignored) {}
    }

    private void loadAvatar(String fromUserId) {
        if (fromUserId == null || fromUserId.trim().isEmpty()) return;

        DatabaseReference usersRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(fromUserId);

        imgAvatar.setTag(fromUserId);
        usersRef.child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object tag = imgAvatar.getTag();
                if (tag == null || !fromUserId.equals(tag.toString())) return;

                String base64 = snapshot.getValue(String.class);
                if (base64 == null || base64.isEmpty()) return;

                Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                if (bmp != null) {
                    // Khi load được ảnh thật, bỏ padding và scale kiểu CROP để lấp đầy hình tròn
                    imgAvatar.setPadding(0, 0, 0, 0);
                    imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgAvatar.setImageBitmap(bmp);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int dp(int v) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }
}