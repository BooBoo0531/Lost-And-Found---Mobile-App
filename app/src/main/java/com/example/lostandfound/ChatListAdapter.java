package com.example.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private Context context;
    private List<String> userIds; // Danh sách ID những người đã chat
    private DatabaseReference usersRef;

    public ChatListAdapter(Context context, List<String> userIds) {
        this.context = context;
        this.userIds = userIds;
        // Link tới bảng Users để lấy tên và avatar
        usersRef = FirebaseDatabase.getInstance("LINK_DATABASE_CUA_BAN").getReference("users");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String targetUserId = userIds.get(position);

        // Lấy thông tin người dùng từ ID
        usersRef.child(targetUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Lấy email hoặc tên (tùy cấu trúc user của bạn)
                    String email = snapshot.child("email").getValue(String.class);
                    String avatarBase64 = snapshot.child("avatarUrl").getValue(String.class);

                    // Hiển thị tên (cắt bớt email cho đẹp)
                    if (email != null) holder.tvUsername.setText(email);

                    // Hiển thị Avatar
                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        try {
                            Bitmap bmp = ImageUtil.base64ToBitmap(avatarBase64);
                            holder.imgAvatar.setImageBitmap(bmp);
                        } catch (Exception e) {}
                    }
                } else {
                    holder.tvUsername.setText("Người dùng ẩn danh");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Click vào dòng -> Mở màn hình chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("TARGET_USER_ID", targetUserId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return userIds.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        ImageView imgAvatar;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}