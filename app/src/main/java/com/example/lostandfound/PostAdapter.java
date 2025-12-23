package com.example.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

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
    private final DatabaseReference notifyRef;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference("users");
        commentsRootRef = db.getReference("comments");
        notifyRef = db.getReference("notifications");
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

        // --- GÁN DỮ LIỆU ---
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
            try {
                Bitmap bmp = ImageUtil.base64ToBitmap(post.getImageBase64());
                if (bmp != null) {
                    holder.imgPostImage.setVisibility(View.VISIBLE);
                    holder.imgPostImage.setImageBitmap(bmp);
                } else {
                    holder.imgPostImage.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                holder.imgPostImage.setVisibility(View.GONE);
            }
        } else {
            holder.imgPostImage.setVisibility(View.GONE);
        }

        // --- LOGIC NÚT LIÊN HỆ (CHAT) ---
        if (isOwner(post)) {
            holder.btnContactUser.setVisibility(View.GONE);
        } else {
            holder.btnContactUser.setVisibility(View.VISIBLE);

            // Bắt sự kiện click
            holder.btnContactUser.setOnClickListener(v -> {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(context, "Bạn cần đăng nhập để nhắn tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                String targetUserId = post.getUserId(); // ID chủ bài viết
                if (targetUserId != null && !targetUserId.isEmpty()) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("TARGET_USER_ID", targetUserId);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
                }
            });
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
                quickSendComment(post, text, () -> holder.etComment.setText(""));
            }
        });

        // --- XỬ LÝ MENU (Nhấn giữ) ---
        holder.itemView.setOnLongClickListener(v -> {
            if (!isOwner(post)) {
                Toast.makeText(context, "Chỉ chủ bài viết mới được Edit / Delete", Toast.LENGTH_SHORT).show();
                return true;
            }
            showOwnerMenu(v, post); // Gọi hàm hiển thị menu mới
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private void quickSendComment(Post post, String content, Runnable onDone) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Toast.makeText(context, "Bạn cần đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        String postId = safe(post.getId());
        if (postId.isEmpty()) return;

        DatabaseReference ref = commentsRootRef.child(postId);
        String commentId = ref.push().getKey();
        if (commentId == null) return;

        Comment c = new Comment(
                commentId, postId, u.getUid(), u.getEmail(),
                content, "", "", System.currentTimeMillis()
        );

        ref.child(commentId).setValue(c).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                if (onDone != null) onDone.run();
                Toast.makeText(context, "Đã gửi bình luận", Toast.LENGTH_SHORT).show();
                sendCommentNotification(post, content);
            } else {
                Toast.makeText(context, "Gửi bình luận thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCommentNotification(Post post, String content) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || post.getUserId() == null) return;
        if (currentUser.getUid().equals(post.getUserId())) return;

        DatabaseReference targetRef = notifyRef.child(post.getUserId());
        String notiId = targetRef.push().getKey();

        NotificationItem noti = new NotificationItem();
        noti.id = notiId;
        noti.toUserId = post.getUserId();
        noti.fromUserId = currentUser.getUid();
        noti.fromEmail = currentUser.getEmail();
        noti.postId = post.getId();
        noti.type = "COMMENT";
        noti.content = content;
        noti.timestamp = System.currentTimeMillis();
        noti.isRead = false;

        if (notiId != null) {
            targetRef.child(notiId).setValue(noti);
        }
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

    // --- ĐÂY LÀ HÀM ĐÃ SỬA (Dùng BottomSheet thay vì PopupMenu) ---
    private void showOwnerMenu(@NonNull View anchor, @NonNull Post post) {
        // Kiểm tra xem context có phải là FragmentActivity không (để lấy SupportFragmentManager)
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;

            // 1. Tạo BottomSheet
            PostOptionsBottomSheet bottomSheet = PostOptionsBottomSheet.newInstance(post);

            // 2. Lắng nghe sự kiện (Edit / Delete)
            bottomSheet.setListener(new PostOptionsBottomSheet.OnOptionClickListener() {
                @Override
                public void onEdit(Post post) {
                    openEditPost(post); // Gọi hàm mở màn hình sửa
                }

                @Override
                public void onDelete(Post post) {
                    confirmDelete(post); // Gọi hàm xác nhận xóa
                }
            });

            // 3. Hiển thị Menu trượt lên
            bottomSheet.show(activity.getSupportFragmentManager(), "POST_OPTIONS");

        } else {
            Toast.makeText(context, "Lỗi: Không thể mở menu (Context không hỗ trợ)", Toast.LENGTH_SHORT).show();
        }
    }
    // -------------------------------------------------------------

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
                Toast.makeText(context, "Xóa thất bại", Toast.LENGTH_SHORT).show();
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
        if (postId == null || postId.trim().isEmpty()) return;
        if (context instanceof FragmentActivity) {
            FragmentActivity fa = (FragmentActivity) context;
            CommentsBottomSheetDialogFragment.newInstance(postId)
                    .show(fa.getSupportFragmentManager(), "COMMENTS_" + postId);
        }
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
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private String safe(String s) { return s == null ? "" : s; }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTime, tvContent, tvStatus;
        TextView tvAddress, tvContact, tvViewAllComments;
        EditText etComment;
        ImageView btnSendComment;
        ImageView imgPostImage, imgAvatar;

        MaterialButton btnContactUser;

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
            btnContactUser = itemView.findViewById(R.id.btnContactUser);
        }
    }
}