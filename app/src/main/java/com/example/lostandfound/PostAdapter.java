package com.example.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String DB_URL =
            "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final Context context;
    private final List<Post> postList;

    private final DatabaseReference usersRef;
    private final DatabaseReference commentsRootRef; // comments/{postId}
    private final DatabaseReference repliesRootRef;  // replies/{postId}/{commentId}/{replyId}
    private final DatabaseReference rootRef;
    private final DatabaseReference notifyRef;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        usersRef = db.getReference("users");
        commentsRootRef = db.getReference("comments");
        repliesRootRef = db.getReference("replies");
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

        holder.tvUserName.setText(safe(post.getUserEmail()));

        String postedAt = safe(post.getTimePosted());
        holder.tvTime.setText(postedAt.isEmpty() ? "" : postedAt);

        holder.tvContent.setText(stripTransactionPlace(post.getDescription()));

        String lf = safe(post.getLostFoundTime());
        if (lf.isEmpty()) lf = safe(post.getTimePosted());

        if (lf.isEmpty() || "Chọn thời gian".equalsIgnoreCase(lf.trim())) {
            holder.tvLostFoundTime.setVisibility(View.GONE);
        } else {
            holder.tvLostFoundTime.setVisibility(View.VISIBLE);
            holder.tvLostFoundTime.setText("⏰ Thời gian nhặt/mất: " + lf);
        }

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

        // image
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

        // contact button
        if (isOwner(post)) {
            holder.btnContactUser.setVisibility(View.GONE);
        } else {
            holder.btnContactUser.setVisibility(View.VISIBLE);
            holder.btnContactUser.setOnClickListener(v -> {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(context, "Bạn cần đăng nhập để nhắn tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                String targetUserId = post.getUserId();
                if (targetUserId != null && !targetUserId.isEmpty()) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("TARGET_USER_ID", targetUserId);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // avatar + comment count (✅ tính cả reply)
        bindAvatar(holder, post.getUserId());
        bindTotalCommentCount(holder, postId);

        // open bottomsheet
        View.OnClickListener open = v -> openComments(postId);
        if (holder.btnOpenComments != null) holder.btnOpenComments.setOnClickListener(open);
        if (holder.tvCommentCountInline != null) holder.tvCommentCountInline.setOnClickListener(open);

        // owner menu
        holder.itemView.setOnLongClickListener(v -> {
            if (!isOwner(post)) {
                Toast.makeText(context, "Chỉ chủ bài viết mới được Edit / Delete", Toast.LENGTH_SHORT).show();
                return true;
            }
            showOwnerMenu(v, post);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return postList == null ? 0 : postList.size();
    }

    // =========================
    // ✅ TOTAL COMMENTS = parents + replies
    // =========================
    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder.parentsCountRef != null && holder.parentsCountListener != null) {
            holder.parentsCountRef.removeEventListener(holder.parentsCountListener);
        }
        if (holder.repliesCountRef != null && holder.repliesCountListener != null) {
            holder.repliesCountRef.removeEventListener(holder.repliesCountListener);
        }

        holder.parentsCountRef = null;
        holder.parentsCountListener = null;
        holder.repliesCountRef = null;
        holder.repliesCountListener = null;
    }

    private void bindTotalCommentCount(@NonNull PostViewHolder holder, String postId) {
        // clear old listeners (recycler reuse)
        if (holder.parentsCountRef != null && holder.parentsCountListener != null) {
            holder.parentsCountRef.removeEventListener(holder.parentsCountListener);
        }
        if (holder.repliesCountRef != null && holder.repliesCountListener != null) {
            holder.repliesCountRef.removeEventListener(holder.repliesCountListener);
        }

        if (holder.tvCommentCountInline == null) return;

        holder.lastParents = 0;
        holder.lastReplies = 0;
        updateCommentCountText(holder);

        if (postId == null || postId.trim().isEmpty()) return;

        // 1) count parents = comments/{postId}
        DatabaseReference parentsRef = commentsRootRef.child(postId);
        holder.parentsCountRef = parentsRef;

        ValueEventListener parentsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                holder.lastParents = (int) snapshot.getChildrenCount();
                updateCommentCountText(holder);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        holder.parentsCountListener = parentsListener;
        parentsRef.addValueEventListener(parentsListener);

        // 2) count replies = sum(childrenCount of replies/{postId}/{commentId})
        DatabaseReference repliesPostRef = repliesRootRef.child(postId);
        holder.repliesCountRef = repliesPostRef;

        ValueEventListener repliesListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalReplies = 0;
                for (DataSnapshot perComment : snapshot.getChildren()) {
                    totalReplies += (int) perComment.getChildrenCount();
                }
                holder.lastReplies = totalReplies;
                updateCommentCountText(holder);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        holder.repliesCountListener = repliesListener;
        repliesPostRef.addValueEventListener(repliesListener);
    }

    private void updateCommentCountText(@NonNull PostViewHolder holder) {
        int total = holder.lastParents + holder.lastReplies;
        holder.tvCommentCountInline.setText("Bình luận (" + total + ")");
    }

    private void openComments(String postId) {
        if (postId == null || postId.trim().isEmpty()) return;
        if (context instanceof FragmentActivity) {
            FragmentActivity fa = (FragmentActivity) context;
            CommentsBottomSheetDialogFragment.newInstance(postId)
                    .show(fa.getSupportFragmentManager(), "COMMENTS_" + postId);
        }
    }

    // =========================
    // AVATAR (giữ như bạn đang dùng)
    // =========================
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

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // =========================
    // OWNER / OPTIONS
    // =========================
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
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;

            PostOptionsBottomSheet bottomSheet = PostOptionsBottomSheet.newInstance(post);
            bottomSheet.setListener(new PostOptionsBottomSheet.OnOptionClickListener() {
                @Override public void onEdit(Post post) { openEditPost(post); }
                @Override public void onDelete(Post post) { confirmDelete(post); }
            });

            bottomSheet.show(activity.getSupportFragmentManager(), "POST_OPTIONS");
        } else {
            Toast.makeText(context, "Lỗi: Không thể mở menu (Context không hỗ trợ)", Toast.LENGTH_SHORT).show();
        }
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
        updates.put("replies/" + postId, null); // ✅ xóa luôn replies của post
        rootRef.updateChildren(updates).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                Toast.makeText(context, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Xóa thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================
    // HELPERS
    // =========================
    private String stripTransactionPlace(String desc) {
        if (desc == null) return "";
        return desc.replaceAll("\\n?\\(\\s*Giao\\s*dịch\\s*tại\\s*:\\s*[^\\)]*\\)", "").trim();
    }

    private String safe(String s) { return s == null ? "" : s; }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTime, tvContent, tvStatus;
        TextView tvAddress, tvContact;
        TextView tvLostFoundTime;

        ImageView imgPostImage, imgAvatar;
        MaterialButton btnContactUser;

        ImageView btnOpenComments;
        TextView tvCommentCountInline;

        // ✅ two listeners
        DatabaseReference parentsCountRef;
        ValueEventListener parentsCountListener;

        DatabaseReference repliesCountRef;
        ValueEventListener repliesCountListener;

        // ✅ cached values
        int lastParents = 0;
        int lastReplies = 0;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvLostFoundTime = itemView.findViewById(R.id.tvLostFoundTime);

            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            imgAvatar = itemView.findViewById(R.id.imgAvatarPost);
            btnContactUser = itemView.findViewById(R.id.btnContactUser);

            btnOpenComments = itemView.findViewById(R.id.btnOpenComments);
            tvCommentCountInline = itemView.findViewById(R.id.tvCommentCountInline);
        }
    }
}