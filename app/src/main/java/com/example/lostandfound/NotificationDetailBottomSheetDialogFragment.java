package com.example.lostandfound;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // ✅ Thêm import Button
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private static final String DB_URL = "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static NotificationDetailBottomSheetDialogFragment newInstance(@NonNull NotificationItem item) {
        NotificationDetailBottomSheetDialogFragment f = new NotificationDetailBottomSheetDialogFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_NOTIFY, item);
        f.setArguments(b);
        return f;
    }

    private NotificationItem item;
    private LinearLayout rootLayout;
    private ImageView imgAvatar;
    private TextView tvName, tvType, tvTime, tvMessage;

    // ===== Post preview =====
    private TextView tvPostLabel;
    private View postPreview;

    private ImageView imgPostAvatar, imgPostImage;
    private Button btnOpenComments; // ✅ Đã sửa thành Button

    private TextView tvPostUserName, tvPostTime, tvPostStatus, tvPostContent;
    private TextView tvPostLostFoundTime, tvPostAddress, tvPostContact, tvPostCommentCount;

    private DatabaseReference postsRef;
    private DatabaseReference usersRef;
    private DatabaseReference commentsRootRef;
    private DatabaseReference repliesRootRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_notification_detail, container, false);

        Object raw = getArguments() != null ? getArguments().getSerializable(ARG_NOTIFY) : null;
        if (raw instanceof NotificationItem) item = (NotificationItem) raw;

        rootLayout = v.findViewById(R.id.rootLayout);
        imgAvatar = v.findViewById(R.id.imgDetailAvatar);
        tvName = v.findViewById(R.id.tvDetailName);
        tvType = v.findViewById(R.id.tvDetailType);
        tvTime = v.findViewById(R.id.tvDetailTime);
        tvMessage = v.findViewById(R.id.tvDetailMessage);

        tvPostLabel = v.findViewById(R.id.tvPostLabel);

        postPreview = v.findViewById(R.id.includePostPreview);

        btnOpenComments = v.findViewById(R.id.btnOpenComments);
        tvPostCommentCount = v.findViewById(R.id.tvCommentCountInline);

        if (postPreview != null) {
            imgPostAvatar = postPreview.findViewById(R.id.imgAvatarPost);
            tvPostUserName = postPreview.findViewById(R.id.tvUserName);
            tvPostTime = postPreview.findViewById(R.id.tvPostTime);
            tvPostStatus = postPreview.findViewById(R.id.tvStatus);
            tvPostContent = postPreview.findViewById(R.id.tvContent);
            tvPostAddress = postPreview.findViewById(R.id.tvAddress);
            tvPostContact = postPreview.findViewById(R.id.tvContact);
            imgPostImage = postPreview.findViewById(R.id.imgPostImage);
        }

        tvPostLostFoundTime = v.findViewById(R.id.tvLostFoundTime);

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        postsRef = db.getReference("posts");
        usersRef = db.getReference("users");
        commentsRootRef = db.getReference("comments");
        repliesRootRef = db.getReference("replies");

        bindData();
        return v;
    }

    private void bindData() {
        if (item == null) return;

        String who = (item.fromEmail == null || item.fromEmail.trim().isEmpty()) ? "Ai đó" : item.fromEmail;
        tvName.setText(who);

        String typeText = "COMMENT".equalsIgnoreCase(item.type) ? "Bình luận"
                : ("REPLY".equalsIgnoreCase(item.type) ? "Trả lời" : "Thông báo");
        tvType.setText(typeText);

        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(item.timestamp));
        tvTime.setText(time);
        tvMessage.setText(item.content == null ? "" : item.content);

        imgAvatar.setImageResource(R.drawable.ic_notification);
        loadAvatar(item.fromUserId);

        bindPostPreview(item.postId);
    }

    private void openCommentsSheet() {
        try {
            CommentsBottomSheetDialogFragment.newInstance(item.postId)
                    .show(requireActivity().getSupportFragmentManager(), "COMMENTS_" + item.postId);
            dismiss();
        } catch (Exception ignored) {}
    }

    private void bindPostPreview(@Nullable String postId) {
        if (postPreview == null) return;
        String pid = (postId == null) ? "" : postId.trim();

        if (pid.isEmpty() || postsRef == null) {
            postPreview.setVisibility(View.GONE);
            if (tvPostLabel != null) tvPostLabel.setVisibility(View.GONE);
            return;
        }

        postPreview.setVisibility(View.VISIBLE);
        if (tvPostLabel != null) tvPostLabel.setVisibility(View.VISIBLE);

        View.OnClickListener open = v -> openCommentsSheet();
        if (btnOpenComments != null) btnOpenComments.setOnClickListener(open);
        postPreview.setOnClickListener(open);

        if (tvPostCommentCount != null) tvPostCommentCount.setText("Bình luận");

        postsRef.child(pid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Post p = snapshot.getValue(Post.class);
                if (p == null) return;

                if (p.getId() == null || p.getId().trim().isEmpty()) p.setId(pid);

                if (tvPostUserName != null) tvPostUserName.setText(safe(p.getUserEmail()));
                if (tvPostTime != null) tvPostTime.setText(safe(p.getTimePosted()));
                if (tvPostContent != null) tvPostContent.setText(stripTransactionPlace(p.getDescription()));

                if (tvPostStatus != null) {
                    if ("LOST".equalsIgnoreCase(p.getPostType())) {
                        tvPostStatus.setText("LOST");
                        tvPostStatus.setTextColor(0xFFD32F2F);
                    } else {
                        tvPostStatus.setText("FOUND");
                        tvPostStatus.setTextColor(0xFF388E3C);
                    }
                }

                String lf = safe(p.getLostFoundTime());
                if (lf.isEmpty()) lf = safe(p.getTimePosted());
                if (tvPostLostFoundTime != null) {
                    tvPostLostFoundTime.setVisibility(View.VISIBLE);
                    tvPostLostFoundTime.setText("⏰ Thời gian: " + lf);
                }

                if (tvPostAddress != null) {
                    String addr = safe(p.getAddress());
                    if (!addr.isEmpty()) tvPostAddress.setText("📍 " + addr);
                }

                bindPostAvatar(imgPostAvatar, p.getUserId());
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        bindCommentCount(pid);
    }

    private void bindCommentCount(@NonNull String postId) {
        if (commentsRootRef == null || repliesRootRef == null || tvPostCommentCount == null) return;
        commentsRootRef.child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                tvPostCommentCount.setText(count + " bình luận");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindPostAvatar(@Nullable ImageView img, @Nullable String uid) {
        if (img == null || uid == null) return;
        usersRef.child(uid).child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String b64 = snapshot.getValue(String.class);
                if (b64 != null) {
                    Bitmap bmp = ImageUtil.base64ToBitmap(b64);
                    if (bmp != null) img.setImageBitmap(bmp);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String stripTransactionPlace(String desc) {
        if (desc == null) return "";
        return desc.replaceAll("\\n?\\(\\s*Giao\\s*dịch\\s*tại\\s*:\\s*[^\\)]*\\)", "").trim();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void loadAvatar(String fromUserId) {
        if (fromUserId == null) return;
        usersRef.child(fromUserId).child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String b64 = snapshot.getValue(String.class);
                if (b64 != null) {
                    Bitmap bmp = ImageUtil.base64ToBitmap(b64);
                    if (bmp != null) imgAvatar.setImageBitmap(bmp);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int dp(int v) {
        if (getContext() == null) return v;
        float d = getContext().getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }
}