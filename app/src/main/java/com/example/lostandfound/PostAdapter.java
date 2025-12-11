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

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;

    // --- SỬA LỖI 1: Thêm Constructor nhận tham số để khớp với Fragment ---
    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Lưu ý: Đổi lại thành 'item_post' cho đẹp (file XML CardView mình gửi bài trước)
        // Nếu bạn muốn dùng 'item_post_map' thì phải chắc chắn file đó tồn tại
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_map, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Gán dữ liệu chữ
        holder.tvUserName.setText(post.getUserName());
        holder.tvTime.setText(post.getTimePosted());
        holder.tvContent.setText(post.getContent());
        holder.tvStatus.setText(post.getStatus());

        // Xử lý màu sắc
        if ("LOST".equalsIgnoreCase(post.getStatus())) {
            holder.tvStatus.setTextColor(0xFFD32F2F); // Đỏ
            holder.tvStatus.setBackgroundColor(0xFFFFEBEE);
        } else {
            holder.tvStatus.setTextColor(0xFF388E3C); // Xanh lá
            holder.tvStatus.setBackgroundColor(0xFFE8F5E9);
        }

        // --- SỬA LỖI 2: Thêm code hiển thị ảnh Base64 ---
        if (post.getImageBase64() != null && !post.getImageBase64().isEmpty()) {
            holder.imgPostImage.setVisibility(View.VISIBLE);
            try {
                Bitmap bitmap = ImageUtil.base64ToBitmap(post.getImageBase64());
                holder.imgPostImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            holder.imgPostImage.setVisibility(View.GONE);
        }

        // Sự kiện gửi bình luận (Giữ nguyên của bạn)
        holder.btnSendComment.setOnClickListener(v -> {
            String comment = holder.etComment.getText().toString();
            if (!comment.isEmpty()) {
                Toast.makeText(context, "Đã gửi: " + comment, Toast.LENGTH_SHORT).show();
                holder.etComment.setText("");
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTime, tvContent, tvStatus;
        EditText etComment;
        ImageView btnSendComment;
        ImageView imgPostImage; // Thêm biến hiển thị ảnh bài đăng

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ ID (Phải khớp với file item_post.xml)
            tvUserName = itemView.findViewById(R.id.tvUserName); // Khớp
            tvTime = itemView.findViewById(R.id.tvPostTime);     // Khớp
            tvContent = itemView.findViewById(R.id.tvContent);   // Khớp
            tvStatus = itemView.findViewById(R.id.tvStatus);     // Khớp
            etComment = itemView.findViewById(R.id.etComment);   // Khớp
            btnSendComment = itemView.findViewById(R.id.btnSendComment); // Khớp
            // Nếu dùng layout item_post mình gửi bài trước thì chưa có nút comment
            // Bạn có thể xóa dòng này nếu chưa thêm vào XML
            // etComment = itemView.findViewById(R.id.etComment);
            // btnSendComment = itemView.findViewById(R.id.btnSendComment);

            // Quan trọng: Ánh xạ ảnh
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
        }
    }
}