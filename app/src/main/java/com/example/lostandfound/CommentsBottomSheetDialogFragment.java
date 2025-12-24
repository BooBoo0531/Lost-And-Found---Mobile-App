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
import java.util.List;

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

    private DatabaseReference commentsRef;         // comments/{postId}
    private DatabaseReference repliesRootRef;      // replies root
    private DatabaseReference usersRef;
    private DatabaseReference postsRef;
    private DatabaseReference notificationsRootRef;

    private EditText et;
    private ImageView btnPick, btnSend, imgPreview;
    private String pickedImageBase64 = "";

    private LinearLayout replyBar;
    private TextView tvReplyingTo, btnCancelReply;

    private String replyToCommentId = "";
    private String replyToName = "";
    private String replyToUserId = "";

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
        repliesRootRef = db.getReference("replies"); // replies/{postId}/{commentId}/{replyId}
        usersRef = db.getReference("users");
        postsRef = db.getReference("posts");
        notificationsRootRef = db.getReference("notifications");

        rv = v.findViewById(R.id.rvComments);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CommentAdapter(
                requireContext(),
                postId,
                displayList,
                usersRef,
                repliesRootRef,
                c -> {
                    replyToCommentId = (c.id == null) ? "" : c.id;
                    replyToName = (c.userEmail == null) ? "" : c.userEmail;
                    replyToUserId = (c.userId == null) ? "" : c.userId;

                    replyBar.setVisibility(View.VISIBLE);
                    tvReplyingTo.setText("Đang trả lời: " + replyToName);
                }
        );
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
        btnSend.setOnClickListener(x -> onSend());

        loadPostOwner();
        listenParentComments();

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
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ✅ chỉ load COMMENT CHA (parentId rỗng)
    private void listenParentComments() {
        commentsRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        displayList.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Comment c = child.getValue(Comment.class);
                            if (c == null) continue;

                            if (c.id == null || c.id.trim().isEmpty()) c.id = child.getKey();

                            if ((c.content == null || c.content.trim().isEmpty())
                                    && child.child("text").exists()) {
                                String oldText = child.child("text").getValue(String.class);
                                if (oldText != null) c.content = oldText;
                            }

                            String pid = (c.parentId == null) ? "" : c.parentId.trim();
                            if (pid.isEmpty()) {
                                displayList.add(c);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void onSend() {
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

        if (TextUtils.isEmpty(replyToCommentId)) {
            sendParentComment(me, content);
        } else {
            sendReply(me, content, replyToCommentId);
        }
    }

    private void sendParentComment(@NonNull FirebaseUser me, @NonNull String content) {
        String id = commentsRef.push().getKey();
        if (id == null) return;

        Comment c = new Comment(
                id,
                postId,
                me.getUid(),
                me.getEmail(),
                content,
                pickedImageBase64,
                "",
                System.currentTimeMillis()
        );

        commentsRef.child(id).setValue(c)
                .addOnSuccessListener(unused -> {
                    notifyPostOwnerForNewComment(c);

                    et.setText("");
                    pickedImageBase64 = "";
                    imgPreview.setVisibility(View.GONE);
                    clearReplyMode();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi gửi bình luận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("COMMENTS", "sendParentComment failed", e);
                });
    }

    private void sendReply(@NonNull FirebaseUser me, @NonNull String text, @NonNull String commentId) {
        DatabaseReference thisRepliesRef = repliesRootRef.child(postId).child(commentId);
        String rid = thisRepliesRef.push().getKey();
        if (rid == null) return;

        Reply r = new Reply();
        r.id = rid;
        r.userId = me.getUid();
        r.userEmail = me.getEmail();
        r.text = text;
        r.imageBase64 = pickedImageBase64;
        r.createdAt = System.currentTimeMillis();

        thisRepliesRef.child(rid).setValue(r)
                .addOnSuccessListener(unused -> {
                    notifyRepliedUser(commentId, r);

                    et.setText("");
                    pickedImageBase64 = "";
                    imgPreview.setVisibility(View.GONE);
                    clearReplyMode();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi gửi trả lời: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("COMMENTS", "sendReply failed", e);
                });
    }

    private void notifyPostOwnerForNewComment(@NonNull Comment c) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;

        if (TextUtils.isEmpty(postOwnerId) && postsRef != null && !TextUtils.isEmpty(postId)) {
            postsRef.child(postId).child("userId")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String uid = snapshot.getValue(String.class);
                            postOwnerId = (uid == null) ? "" : uid;
                            if (!TextUtils.isEmpty(postOwnerId)) {
                                pushNotification(postOwnerId, "COMMENT", c.id, buildSnippet(c.content, c.imageBase64));
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
            return;
        }

        if (TextUtils.isEmpty(postOwnerId)) return;
        if (postOwnerId.equals(me.getUid())) return;

        pushNotification(postOwnerId, "COMMENT", c.id, buildSnippet(c.content, c.imageBase64));
    }

    private void notifyRepliedUser(@NonNull String commentId, @NonNull Reply r) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;

        commentsRef.child(commentId).child("userId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String toUid = snapshot.getValue(String.class);
                        if (toUid == null || toUid.trim().isEmpty()) return;
                        if (toUid.equals(me.getUid())) return;

                        String snippet = buildSnippet(r.text, r.imageBase64);
                        pushNotification(toUid, "REPLY", commentId, snippet);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void pushNotification(@NonNull String toUserId,
                                  @NonNull String type,
                                  @NonNull String commentId,
                                  @NonNull String snippet) {

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;
        if (notificationsRootRef == null) return;

        String notiId = notificationsRootRef.child(toUserId).push().getKey();
        if (notiId == null) return;

        NotificationItem item = new NotificationItem(
                notiId,
                toUserId,
                me.getUid(),
                me.getEmail(),
                postId,
                commentId,
                type,
                snippet,
                System.currentTimeMillis(),
                false
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
        replyToCommentId = "";
        replyToName = "";
        replyToUserId = "";
        if (replyBar != null) replyBar.setVisibility(View.GONE);
    }
}
