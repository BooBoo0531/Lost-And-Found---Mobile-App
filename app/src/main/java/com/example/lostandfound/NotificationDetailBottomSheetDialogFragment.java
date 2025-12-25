package com.example.lostandfound;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private LinearLayout rootLayout;
    private ImageView imgAvatar;
    private TextView tvName, tvType, tvTime, tvMessage;

    // ===== Post preview (item_post_map) =====
    private TextView tvPostLabel;
    private View postPreview;

    private ImageView imgPostAvatar, imgPostImage, btnOpenComments;
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

        // Views inside item_post_map
        imgPostAvatar = v.findViewById(R.id.imgAvatarPost);
        tvPostUserName = v.findViewById(R.id.tvUserName);
        tvPostTime = v.findViewById(R.id.tvPostTime);
        tvPostStatus = v.findViewById(R.id.tvStatus);
        tvPostContent = v.findViewById(R.id.tvContent);
        tvPostLostFoundTime = v.findViewById(R.id.tvLostFoundTime);
        tvPostAddress = v.findViewById(R.id.tvAddress);
        tvPostContact = v.findViewById(R.id.tvContact);
        imgPostImage = v.findViewById(R.id.imgPostImage);
        btnOpenComments = v.findViewById(R.id.btnOpenComments);
        tvPostCommentCount = v.findViewById(R.id.tvCommentCountInline);

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

        String typeText = "COMMENT".equalsIgnoreCase(item.type)
                ? "Bình luận"
                : ("REPLY".equalsIgnoreCase(item.type) ? "Trả lời" : "Thông báo");
        tvType.setText(typeText);

        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(item.timestamp));
        tvTime.setText(time);

        tvMessage.setText(item.content == null ? "" : item.content);

        imgAvatar.setImageResource(R.drawable.ic_notification);
        imgAvatar.setPadding(dp(6), dp(6), dp(6), dp(6));
        imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loadAvatar(item.fromUserId);

        // ✅ show full post
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
        if (tvPostCommentCount != null) tvPostCommentCount.setOnClickListener(open);
        postPreview.setOnClickListener(open);

        if (tvPostCommentCount != null) tvPostCommentCount.setText("Bình luận");

        postsRef.child(pid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Post p = snapshot.getValue(Post.class);
                if (p == null) {
                    if (tvPostContent != null) tvPostContent.setText("(Bài viết không còn tồn tại)");
                    return;
                }
                if (p.getId() == null || p.getId().trim().isEmpty()) p.setId(pid);

                if (tvPostUserName != null) tvPostUserName.setText(safe(p.getUserEmail()));
                if (tvPostTime != null) tvPostTime.setText(safe(p.getTimePosted()));
                if (tvPostContent != null) tvPostContent.setText(stripTransactionPlace(p.getDescription()));

                if (tvPostStatus != null) {
                    tvPostStatus.setBackgroundResource(R.drawable.bg_label_rounded);
                    if ("LOST".equalsIgnoreCase(p.getPostType())) {
                        tvPostStatus.setText("LOST");
                        tvPostStatus.setTextColor(0xFFD32F2F);
                        tvPostStatus.setBackgroundTintList(ColorStateList.valueOf(0xFFFFEBEE));
                    } else {
                        tvPostStatus.setText("FOUND");
                        tvPostStatus.setTextColor(0xFF388E3C);
                        tvPostStatus.setBackgroundTintList(ColorStateList.valueOf(0xFFE8F5E9));
                    }
                }

                String lf = safe(p.getLostFoundTime());
                if (lf.isEmpty()) lf = safe(p.getTimePosted());
                if (tvPostLostFoundTime != null) {
                    if (lf.isEmpty() || "Chọn thời gian".equalsIgnoreCase(lf.trim())) {
                        tvPostLostFoundTime.setVisibility(View.GONE);
                    } else {
                        tvPostLostFoundTime.setVisibility(View.VISIBLE);
                        tvPostLostFoundTime.setText("⏰ Thời gian nhặt/mất: " + lf);
                    }
                }

                if (tvPostAddress != null) {
                    String addr = safe(p.getAddress());
                    if (addr.isEmpty()) tvPostAddress.setVisibility(View.GONE);
                    else {
                        tvPostAddress.setVisibility(View.VISIBLE);
                        tvPostAddress.setText("📍 Địa điểm: " + addr);
                    }
                }

                if (tvPostContact != null) {
                    String contact = safe(p.getContact());
                    if (contact.isEmpty()) tvPostContact.setVisibility(View.GONE);
                    else {
                        tvPostContact.setVisibility(View.VISIBLE);
                        tvPostContact.setText("☎ Liên hệ: " + contact);
                    }
                }

                if (imgPostImage != null) {
                    String b64 = safe(p.getImageBase64());
                    if (b64.isEmpty()) {
                        imgPostImage.setVisibility(View.GONE);
                    } else {
                        Bitmap bmp = ImageUtil.base64ToBitmap(b64);
                        if (bmp != null) {
                            imgPostImage.setVisibility(View.VISIBLE);
                            imgPostImage.setImageBitmap(bmp);
                        } else imgPostImage.setVisibility(View.GONE);
                    }
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
                int parents = 0;
                for (DataSnapshot c : snapshot.getChildren()) {
                    String pid = c.child("parentId").getValue(String.class);
                    if (pid == null || pid.trim().isEmpty()) parents++;
                }

                int finalParents = parents;
                repliesRootRef.child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        int replies = 0;
                        for (DataSnapshot perComment : snap.getChildren()) {
                            replies += (int) perComment.getChildrenCount();
                        }
                        tvPostCommentCount.setText("Bình luận (" + (finalParents + replies) + ")");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindPostAvatar(@Nullable ImageView img, @Nullable String uid) {
        if (img == null) return;

        img.setImageResource(R.drawable.ic_notification);
        img.setPadding(dp(6), dp(6), dp(6), dp(6));
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        String u = (uid == null) ? "" : uid.trim();
        if (u.isEmpty() || usersRef == null) return;

        img.setTag(u);
        usersRef.child(u).child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object tag = img.getTag();
                if (tag == null || !u.equals(tag.toString())) return;
                String base64 = snapshot.getValue(String.class);
                if (base64 == null || base64.isEmpty()) return;
                Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                if (bmp != null) {
                    img.setPadding(0, 0, 0, 0);
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setImageBitmap(bmp);
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
        if (fromUserId == null || fromUserId.trim().isEmpty()) return;

        DatabaseReference uref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(fromUserId);

        imgAvatar.setTag(fromUserId);
        uref.child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object tag = imgAvatar.getTag();
                if (tag == null || !fromUserId.equals(tag.toString())) return;

                String base64 = snapshot.getValue(String.class);
                if (base64 == null || base64.isEmpty()) return;

                Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                if (bmp != null) {
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
