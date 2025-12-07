package com.example.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;

    public PostAdapter() {
        this.postList = new ArrayList<>();
    }

    // Hàm cập nhật dữ liệu mới
    public void setPostList(List<Post> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Kết nối với file item_post_map.xml bạn vừa tạo
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_map, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Gán dữ liệu vào các View
        holder.tvUserName.setText(post.getUserName());
        holder.tvTime.setText(post.getTimePosted());
        holder.tvContent.setText(post.getContent());
        holder.tvStatus.setText(post.getStatus());

        // Xử lý màu sắc: LOST (Đỏ) - FOUND (Xanh)
        if ("LOST".equalsIgnoreCase(post.getStatus())) {
            holder.tvStatus.setTextColor(0xFFD32F2F); // Màu đỏ
            holder.tvStatus.setBackgroundColor(0xFFFFEBEE); // Nền đỏ nhạt
        } else {
            holder.tvStatus.setTextColor(0xFF388E3C); // Màu xanh lá
            holder.tvStatus.setBackgroundColor(0xFFE8F5E9); // Nền xanh nhạt
        }

        // Sự kiện nút gửi bình luận
        holder.btnSendComment.setOnClickListener(v -> {
            String comment = holder.etComment.getText().toString();
            if (!comment.isEmpty()) {
                Toast.makeText(v.getContext(), "Đã gửi: " + comment, Toast.LENGTH_SHORT).show();
                holder.etComment.setText(""); // Xóa ô nhập sau khi gửi
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // Class nắm giữ các thành phần giao diện (Ánh xạ ID)
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTime, tvContent, tvStatus;
        EditText etComment;
        ImageView btnSendComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            etComment = itemView.findViewById(R.id.etComment);
            btnSendComment = itemView.findViewById(R.id.btnSendComment);
        }
    }
}