package com.example.lostandfound;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
    private static final String DB_URL = "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static CommentsBottomSheetDialogFragment newInstance(String postId) {
        CommentsBottomSheetDialogFragment f = new CommentsBottomSheetDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_POST_ID, postId);
        f.setArguments(b);
        return f;
    }

    private String postId;

    private RecyclerView rv;
    private final List<Comment> displayList = new ArrayList<>();
    private CommentAdapter adapter;

    private DatabaseReference commentsRef;
    private DatabaseReference usersRef;

    private EditText et;
    private ImageView btnPick, btnSend, imgPreview;
    private String pickedImageBase64 = "";

    private LinearLayout replyBar;
    private TextView tvReplyingTo, btnCancelReply;
    private String replyToId = "";
    private String replyToName = "";

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

        commentsRef = FirebaseDatabase.getInstance(DB_URL).getReference("comments").child(postId);
        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("users");

        rv = v.findViewById(R.id.rvComments);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommentAdapter(requireContext(), displayList, usersRef, c -> {
            // reply mode
            replyToId = c.id;
            replyToName = c.userEmail == null ? "" : c.userEmail;
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

        listenComments();

        return v;
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
                            // đảm bảo id
                            if (c.id == null || c.id.trim().isEmpty()) c.id = child.getKey();
                            all.add(c);
                        }

                        // build display list: parent -> replies
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
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
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

        Comment c = new Comment(
                id,
                postId,
                u.getUid(),
                u.getEmail(),
                content,
                pickedImageBase64,
                (replyToId == null ? "" : replyToId),
                System.currentTimeMillis()
        );

        commentsRef.child(id).setValue(c).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                et.setText("");
                pickedImageBase64 = "";
                imgPreview.setVisibility(View.GONE);
                clearReplyMode();
            } else {
                Toast.makeText(getContext(), "Lỗi gửi bình luận", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearReplyMode() {
        replyToId = "";
        replyToName = "";
        if (replyBar != null) replyBar.setVisibility(View.GONE);
    }
}
