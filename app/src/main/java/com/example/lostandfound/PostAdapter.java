package com.example.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String DB_URL = "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final Context context;
    private final List<Post> postList;

    private final DatabaseReference usersRef;
    private final DatabaseReference commentsRootRef;
    private final DatabaseReference rootRef;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference("users");
        commentsRootRef = db.getReference("comments");
        rootRef = db.getReference();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_post_map, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        String postId = safe(post.getId());

        holder.tvUserName.setText(safe(post.getUserEmail()));
        holder.tvTime.setText(safe(post.getTimePosted()));
        holder.tvContent.setText(safe(post.getDescription()));

        String addr = safe(post.getAddress());
        if (addr.isEmpty()) {
            holder.tvAddress.setVisibility(View.GONE);
        } else {
            holder.tvAddress.setVisibility(View.VISIBLE);
            holder.tvAddress.setText("📍 Địa điểm: " + addr);
        }

        String contact = safe(post.getContact());
        if (contact.isEmpty()) {
            holder.tvContact.setVisibility(View.GONE);
        } else {
            holder.tvContact.setVisibility(View.VISIBLE);
            holder.tvContact.setText("☎ Liên hệ: " + contact);
        }

        if ("LOST".equalsIgnoreCase(post.getPostType())) {
            holder.tvStatus.setText("LOST");
            holder.tvStatus.setTextColor(0xFFD32F2F);
            holder.tvStatus.setBackgroundColor(0xFFFFEBEE);
        } else {
            holder.tvStatus.setText("FOUND");
            holder.tvStatus.setTextColor(0xFF388E3C);
            holder.tvStatus.setBackgroundColor(0xFFE8F5E9);
        }

        if (post.getImageBase64() != null && !post.getImageBase64().isEmpty()) {
            Bitmap bmp = ImageUtil.base64ToBitmap(post.getImageBase64());
            if (bmp != null) {
                holder.imgPostImage.setVisibility(View.VISIBLE);
                holder.imgPostImage.setImageBitmap(bmp);
            } else {
                holder.imgPostImage.setVisibility(View.GONE);
            }
        } else {
            holder.imgPostImage.setVisibility(View.GONE);
        }

        bindAvatar(holder, post.getUserId());

        bindCommentCount(holder, postId);

        View.OnClickListener open = v -> openComments(postId);
        holder.tvViewAllComments.setOnClickListener(open);
        holder.etComment.setOnClickListener(open);

        holder.btnSendComment.setOnClickListener(v -> {
            String text = holder.etComment.getText() != null ? holder.etComment.getText().toString().trim() : "";
            if (text.isEmpty()) {
                openComments(postId);
            } else {
                quickSendComment(postId, text, () -> holder.etComment.setText(""));
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isOwner(post)) {
                Toast.makeText(context, "Chỉ chủ bài viết mới được Edit / Delete", Toast.LENGTH_SHORT).show();
                return true;
            }
            showOwnerMenu(v, post);
            return true;
        });
    }

    private boolean isOwner(Post post) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || post == null) return false;

        String uid = u.getUid();
        String postUid = safe(post.getUserId());
        if (!postUid.isEmpty() && postUid.equals(uid)) return true;

        String email = safe(u.getEmail());
        String postEmail = safe(post.getUserEmail());
        return !email.isEmpty() && email.equalsIgnoreCase(postEmail);
    }

    private void showOwnerMenu(@NonNull View anchor, @NonNull Post post) {
        PopupMenu popup = new PopupMenu(context, anchor);

        final int MENU_EDIT = 1;
        final int MENU_DELETE = 2;

        popup.getMenu().add(0, MENU_EDIT, 0, "Edit");
        popup.getMenu().add(0, MENU_DELETE, 1, "Delete post");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == MENU_EDIT) {
                openEditPost(post);
                return true;
            } else if (id == MENU_DELETE) {
                confirmDelete(post);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void openEditPost(@NonNull Post post) {
        Intent i = new Intent(context, PostActivity.class);
        i.putExtra(PostActivity.EXTRA_MODE, PostActivity.MODE_EDIT);
        i.putExtra(PostActivity.EXTRA_POST, post);

        i.putExtra("POST_TYPE", safe(post.getPostType()));
        context.startActivity(i);
    }

    private void confirmDelete(@NonNull Post post) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn chắc chắn muốn xóa bài viết này không?\n(Hành động này không thể hoàn tác)")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> deletePost(post))
                .show();
    }

    private void deletePost(@NonNull Post post) {
        String postId = safe(post.getId());
        if (postId.isEmpty()) {
            Toast.makeText(context, "Không tìm thấy postId để xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isOwner(post)) {
            Toast.makeText(context, "Bạn không có quyền xóa bài viết này", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("posts/" + postId, null);
        updates.put("comments/" + postId, null);

        rootRef.updateChildren(updates).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                Toast.makeText(context, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context,
                        "Xóa thất bại: " + (t.getException() != null ? t.getException().getMessage() : "Không rõ"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.commentCountRef != null && holder.commentCountListener != null) {
            holder.commentCountRef.removeEventListener(holder.commentCountListener);
        }
        holder.commentCountRef = null;
        holder.commentCountListener = null;
    }

    private void bindCommentCount(@NonNull PostViewHolder holder, String postId) {
        if (holder.commentCountRef != null && holder.commentCountListener != null) {
            holder.commentCountRef.removeEventListener(holder.commentCountListener);
        }

        if (postId.isEmpty()) {
            holder.tvViewAllComments.setText("Xem tất cả bình luận (0)");
            return;
        }

        DatabaseReference ref = commentsRootRef.child(postId);
        holder.commentCountRef = ref;

        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                holder.tvViewAllComments.setText("Xem tất cả bình luận (" + count + ")");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        holder.commentCountListener = l;

        ref.addValueEventListener(l);
    }

    private void openComments(String postId) {
        if (postId == null || postId.trim().isEmpty()) {
            Toast.makeText(context, "Bài viết thiếu ID (postId)", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(context instanceof FragmentActivity)) {
            Toast.makeText(context, "Context không phải FragmentActivity", Toast.LENGTH_SHORT).show();
            return;
        }
        FragmentActivity fa = (FragmentActivity) context;
        CommentsBottomSheetDialogFragment.newInstance(postId)
                .show(fa.getSupportFragmentManager(), "COMMENTS_" + postId);
    }

    private void quickSendComment(String postId, String content, Runnable onDone) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Toast.makeText(context, "Bạn cần đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }
        if (postId == null || postId.trim().isEmpty()) return;

        DatabaseReference ref = commentsRootRef.child(postId);
        String id = ref.push().getKey();
        if (id == null) return;

        Comment c = new Comment(
                id, postId, u.getUid(), u.getEmail(),
                content, "", "", System.currentTimeMillis()
        );

        ref.child(id).setValue(c).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                if (onDone != null) onDone.run();
            } else {
                Toast.makeText(context, "Gửi bình luận thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindAvatar(@NonNull PostViewHolder holder, String userId) {
        holder.imgAvatar.setImageResource(R.drawable.ic_notification);
        holder.imgAvatar.setPadding(15, 15, 15, 15);
        holder.imgAvatar.setColorFilter(0xFF888888);
        holder.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        holder.imgAvatar.setTag(userId);

        if (userId == null || userId.trim().isEmpty() || usersRef == null) return;

        usersRef.child(userId).child("avatarUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object tag = holder.imgAvatar.getTag();
                        if (tag == null || !userId.equals(tag.toString())) return;

                        if (!snapshot.exists()) return;

                        String avatarBase64 = snapshot.getValue(String.class);
                        if (avatarBase64 == null || avatarBase64.isEmpty()) return;

                        try {
                            Bitmap avatarBmp = ImageUtil.base64ToBitmap(avatarBase64);
                            if (avatarBmp != null) {
                                holder.imgAvatar.clearColorFilter();
                                holder.imgAvatar.setImageTintList(null);
                                holder.imgAvatar.setPadding(0, 0, 0, 0);
                                holder.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                holder.imgAvatar.setImageBitmap(avatarBmp);
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private String safe(String s) { return s == null ? "" : s; }

    @Override
    public int getItemCount() { return postList.size(); }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvUserName, tvTime, tvContent, tvStatus;
        TextView tvAddress, tvContact, tvViewAllComments;
        EditText etComment;
        ImageView btnSendComment;
        ImageView imgPostImage, imgAvatar;

        DatabaseReference commentCountRef;
        ValueEventListener commentCountListener;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvViewAllComments = itemView.findViewById(R.id.tvViewAllComments);

            etComment = itemView.findViewById(R.id.etComment);
            btnSendComment = itemView.findViewById(R.id.btnSendComment);

            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            imgAvatar = itemView.findViewById(R.id.imgAvatarPost);
        }
    }
}
