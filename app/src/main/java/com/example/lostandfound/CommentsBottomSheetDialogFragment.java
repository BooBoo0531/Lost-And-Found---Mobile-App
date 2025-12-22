package com.example.lostandfound;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_POST_ID = "ARG_POST_ID";
    private static final String DB_URL =
            "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static CommentsBottomSheetDialogFragment newInstance(String postId) {
        CommentsBottomSheetDialogFragment f = new CommentsBottomSheetDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_POST_ID, postId);
        f.setArguments(b);
        return f;
    }

    private String postId = "";

    private RecyclerView rv;
    private final List<Comment> displayList = new ArrayList<>();
    private CommentAdapter adapter;

    private DatabaseReference commentsRef;
    private DatabaseReference usersRef;
    private DatabaseReference postsRef;
    private DatabaseReference notificationsRootRef;

    private EditText et;
    private ImageView btnPick, btnSend, imgPreview;
    private String pickedImageBase64 = "";

    private LinearLayout replyBar;
    private TextView tvReplyingTo, btnCancelReply;

    // reply state
    private String replyToId = "";
    private String replyToName = "";
    private String replyToUserId = "";

    // post owner cache
    private String postOwnerId = "";

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                try {
                    Bitmap bmp = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                    pickedImageBase64 = ImageUtil.bitmapToBase64(bmp);
                    imgPreview.setVisibility(View.VISIBLE);
                    imgPreview.setImageBitmap(bmp);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Không đọc được ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_comments_bottom_sheet, container, false);

        postId = getArguments() != null ? getArguments().getString(ARG_POST_ID, "") : "";
        if (postId == null) postId = "";

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);

        commentsRef = db.getReference("comments").child(postId);
        usersRef = db.getReference("users");
        postsRef = db.getReference("posts");
        notificationsRootRef = db.getReference("notifications");

        rv = v.findViewById(R.id.rvComments);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CommentAdapter(requireContext(), displayList, usersRef, c -> {
            // reply mode
            replyToId = (c.id == null) ? "" : c.id;
            replyToName = (c.userEmail == null) ? "" : c.userEmail;
            replyToUserId = (c.userId == null) ? "" : c.userId;

            replyBar.setVisibility(View.VISIBLE);
            tvReplyingTo.setText("Đang trả lời: " + replyToName);
        });
        rv.setAdapter(adapter);

        et = v.findViewById(R.id.etCommentContent);
        btnPick = v.findViewById(R.id.btnPickImage);
        btnSend = v.findViewById(R.id.btnSend);
        imgPreview = v.findViewById(R.id.imgPreview);

        replyBar = v.findViewById(R.id.replyBar);
        tvReplyingTo = v.findViewById(R.id.tvReplyingTo);
        btnCancelReply = v.findViewById(R.id.btnCancelReply);

        btnCancelReply.setOnClickListener(x -> clearReplyMode());
        btnPick.setOnClickListener(x -> pickImageLauncher.launch("image/*"));
        btnSend.setOnClickListener(x -> sendComment());

        loadPostOwner();
        listenComments();

        return v;
    }

    private void loadPostOwner() {
        if (postsRef == null || TextUtils.isEmpty(postId)) return;

        postsRef.child(postId).child("userId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String uid = snapshot.getValue(String.class);
                        postOwnerId = (uid == null) ? "" : uid;
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void listenComments() {
        commentsRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        List<Comment> all = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Comment c = child.getValue(Comment.class);
                            if (c == null) continue;

                            if (c.id == null || c.id.trim().isEmpty()) c.id = child.getKey();

                            // fallback DB cũ lưu "text"
                            if ((c.content == null || c.content.trim().isEmpty())
                                    && child.child("text").exists()) {
                                String oldText = child.child("text").getValue(String.class);
                                if (oldText != null) c.content = oldText;
                            }

                            all.add(c);
                        }

                        Map<String, List<Comment>> replies = new HashMap<>();
                        List<Comment> parents = new ArrayList<>();

                        for (Comment c : all) {
                            if (c.parentId == null || c.parentId.trim().isEmpty()) {
                                parents.add(c);
                            } else {
                                replies.computeIfAbsent(c.parentId, k -> new ArrayList<>()).add(c);
                            }
                        }

                        displayList.clear();
                        for (Comment p : parents) {
                            displayList.add(p);
                            List<Comment> rs = replies.get(p.id);
                            if (rs != null) displayList.addAll(rs);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendComment() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = et.getText() != null ? et.getText().toString().trim() : "";
        if (content.isEmpty() && (pickedImageBase64 == null || pickedImageBase64.isEmpty())) {
            Toast.makeText(getContext(), "Nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = commentsRef.push().getKey();
        if (id == null) return;

        String parentId = (replyToId == null) ? "" : replyToId;

        Comment c = new Comment(
                id,
                postId,
                me.getUid(),
                me.getEmail(),
                content,
                pickedImageBase64,
                parentId,
                System.currentTimeMillis()
        );

        commentsRef.child(id).setValue(c)
                .addOnSuccessListener(unused -> {
                    // ✅ gửi notify sau khi comment thành công
                    if (TextUtils.isEmpty(parentId)) {
                        notifyPostOwnerForNewComment(c);
                    } else {
                        notifyRepliedUser(c);
                    }

                    et.setText("");
                    pickedImageBase64 = "";
                    imgPreview.setVisibility(View.GONE);
                    clearReplyMode();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi gửi bình luận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("COMMENTS", "sendComment failed", e);
                });
    }

    // ===================== NOTIFICATIONS =====================

    private void notifyPostOwnerForNewComment(@NonNull Comment c) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;

        // Nếu chưa có postOwnerId thì đọc lại
        if (TextUtils.isEmpty(postOwnerId) && postsRef != null && !TextUtils.isEmpty(postId)) {
            postsRef.child(postId).child("userId")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String uid = snapshot.getValue(String.class);
                            postOwnerId = (uid == null) ? "" : uid;
                            if (!TextUtils.isEmpty(postOwnerId)) pushNotification(postOwnerId, "COMMENT", c);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                    });
            return;
        }

        if (TextUtils.isEmpty(postOwnerId)) return;

        // ✅ test nhớ: nếu bạn tự comment bài của bạn thì sẽ KHÔNG notify (tránh tự spam)
        if (postOwnerId.equals(me.getUid())) return;

        pushNotification(postOwnerId, "COMMENT", c);
    }

    private void notifyRepliedUser(@NonNull Comment c) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;

        // replyToUserId lấy từ comment được bấm "Trả lời"
        if (TextUtils.isEmpty(replyToUserId)) {
            // fallback: đọc parent comment để lấy userId
            if (!TextUtils.isEmpty(c.parentId)) {
                commentsRef.child(c.parentId).child("userId")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String uid = snapshot.getValue(String.class);
                                if (uid == null || uid.trim().isEmpty()) return;
                                if (uid.equals(me.getUid())) return;
                                pushNotification(uid, "REPLY", c);
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) { }
                        });
            }
            return;
        }

        if (replyToUserId.equals(me.getUid())) return;

        pushNotification(replyToUserId, "REPLY", c);
    }

    private void pushNotification(@NonNull String toUserId, @NonNull String type, @NonNull Comment c) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;
        if (notificationsRootRef == null) return;

        String notiId = notificationsRootRef.child(toUserId).push().getKey();
        if (notiId == null) return;

        String snippet = buildSnippet(c.content, c.imageBase64);

        NotificationItem item = new NotificationItem(
                notiId,
                toUserId,
                me.getUid(),
                me.getEmail(),
                postId,
                c.id,
                type,
                snippet,
                System.currentTimeMillis(),
                false // ✅ isRead=false để badge hoạt động
        );

        notificationsRootRef.child(toUserId).child(notiId).setValue(item)
                .addOnFailureListener(e -> Log.e("COMMENTS", "pushNotification failed: " + e.getMessage(), e));
    }

    private String buildSnippet(String text, String imageBase64) {
        String s = (text == null) ? "" : text.trim();
        if (s.isEmpty() && imageBase64 != null && !imageBase64.trim().isEmpty()) {
            s = "🖼 Đã gửi 1 ảnh";
        }
        if (s.length() > 80) s = s.substring(0, 80) + "...";
        return s;
    }

    private void clearReplyMode() {
        replyToId = "";
        replyToName = "";
        replyToUserId = "";
        if (replyBar != null) replyBar.setVisibility(View.GONE);
    }
}
