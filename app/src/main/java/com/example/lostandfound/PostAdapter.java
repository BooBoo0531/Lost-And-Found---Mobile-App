package com.example.lostandfound;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;

    // Thêm biến tham chiếu đến bảng Users
    private DatabaseReference usersRef;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        // Khởi tạo kết nối đến bảng users để tra cứu avatar
        try {
            usersRef = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_map, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // 1. Gán thông tin chữ
        holder.tvUserName.setText(post.getUserEmail());
        holder.tvTime.setText(post.getTimePosted());
        holder.tvContent.setText(post.getDescription());

        // 2. Xử lý LOST/FOUND (Giữ nguyên)
        if ("LOST".equalsIgnoreCase(post.getPostType())) {
            holder.tvStatus.setText("LOST");
            holder.tvStatus.setTextColor(0xFFD32F2F);
            holder.tvStatus.setBackgroundColor(0xFFFFEBEE);
        } else {
            holder.tvStatus.setText("FOUND");
            holder.tvStatus.setTextColor(0xFF388E3C);
            holder.tvStatus.setBackgroundColor(0xFFE8F5E9);
        }

        // 3. Ảnh bài đăng (Giữ nguyên)
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

        // --- 4. XỬ LÝ AVATAR (CODE ĐÃ NÂNG CẤP) ---

        // A. Đặt trạng thái mặc định (Icon xám)
        holder.imgAvatar.setImageResource(R.drawable.ic_notification);
        holder.imgAvatar.setPadding(15, 15, 15, 15); // Padding để icon nhỏ lại xíu
        holder.imgAvatar.setColorFilter(0xFF888888); // Tô màu xám thủ công bằng code

        String userId = post.getUserId();

        // Log kiểm tra (Xem Logcat để biết bài này có ID không)
        android.util.Log.d("CHECK_AVATAR", "Email: " + post.getUserEmail() + " | ID: " + userId);

        if (userId != null && !userId.isEmpty() && usersRef != null) {
            usersRef.child(userId).child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String avatarBase64 = snapshot.getValue(String.class);
                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                            try {
                                Bitmap avatarBmp = ImageUtil.base64ToBitmap(avatarBase64);
                                if (avatarBmp != null) {
                                    // B. Có ảnh -> XÓA MÀU XÁM, XÓA PADDING
                                    holder.imgAvatar.clearColorFilter(); // Quan trọng: Xóa lớp màu xám
                                    holder.imgAvatar.setImageTintList(null); // Xóa tint nếu có

                                    holder.imgAvatar.setPadding(0, 0, 0, 0); // Full khung
                                    holder.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    holder.imgAvatar.setImageBitmap(avatarBmp);
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTime, tvContent, tvStatus;
        EditText etComment;
        ImageView btnSendComment, imgPostImage;
        ImageView imgAvatar; // Ảnh đại diện nhỏ

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            etComment = itemView.findViewById(R.id.etComment);
            btnSendComment = itemView.findViewById(R.id.btnSendComment);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);

            // Ánh xạ avatar (đảm bảo ID này đúng trong item_post_map.xml)
            imgAvatar = itemView.findViewById(R.id.imgAvatarPost);
        }
    }
}