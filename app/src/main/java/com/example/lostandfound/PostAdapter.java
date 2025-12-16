package com.example.lostandfound;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList;

    private DatabaseReference usersRef;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;

        try {
            usersRef = FirebaseDatabase
                    .getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users");
        } catch (Exception e) {
            e.printStackTrace();
            usersRef = null;
        }
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

        // 1) Text
        holder.tvUserName.setText(safe(post.getUserEmail()));
        holder.tvTime.setText(safe(post.getTimePosted()));
        holder.tvContent.setText(safe(post.getDescription()));

        // 2) LOST / FOUND
        if ("LOST".equalsIgnoreCase(post.getPostType())) {
            holder.tvStatus.setText("LOST");
            holder.tvStatus.setTextColor(0xFFD32F2F);
            holder.tvStatus.setBackgroundColor(0xFFFFEBEE);
        } else {
            holder.tvStatus.setText("FOUND");
            holder.tvStatus.setTextColor(0xFF388E3C);
            holder.tvStatus.setBackgroundColor(0xFFE8F5E9);
        }

        // 3) Ảnh bài đăng
        if (post.getImageBase64() != null && !post.getImageBase64().isEmpty()) {
            holder.imgPostImage.setVisibility(View.VISIBLE);
            try {
                Bitmap bitmap = ImageUtil.base64ToBitmap(post.getImageBase64());
                holder.imgPostImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.imgPostImage.setVisibility(View.GONE);
            }
        } else {
            holder.imgPostImage.setVisibility(View.GONE);
        }

        // 4) Avatar
        bindAvatar(holder, post.getUserId());
    }

    private void bindAvatar(@NonNull PostViewHolder holder, String userId) {
        // A) trạng thái mặc định
        holder.imgAvatar.setImageResource(R.drawable.ic_notification);
        holder.imgAvatar.setPadding(15, 15, 15, 15);
        holder.imgAvatar.setColorFilter(0xFF888888);
        holder.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // tag để chống recycle sai ảnh
        holder.imgAvatar.setTag(userId);

        if (userId == null || userId.trim().isEmpty() || usersRef == null) return;

        usersRef.child(userId).child("avatarUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // holder đã bị recycle sang item khác
                        Object tag = holder.imgAvatar.getTag();
                        if (tag == null || !userId.equals(tag.toString())) return;

                        if (!snapshot.exists()) return;

                        String avatarBase64 = snapshot.getValue(String.class);
                        if (avatarBase64 == null || avatarBase64.isEmpty()) return;

                        try {
                            Bitmap avatarBmp = ImageUtil.base64ToBitmap(avatarBase64);
                            if (avatarBmp != null) {
                                // B) có ảnh -> bỏ xám/padding
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

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public int getItemCount() {
        return postList == null ? 0 : postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTime, tvContent, tvStatus;
        EditText etComment;
        ImageView btnSendComment, imgPostImage;
        ImageView imgAvatar;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            etComment = itemView.findViewById(R.id.etComment);
            btnSendComment = itemView.findViewById(R.id.btnSendComment);

            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            imgAvatar = itemView.findViewById(R.id.imgAvatarPost); // nhớ đúng id trong XML
        }
    }
}
