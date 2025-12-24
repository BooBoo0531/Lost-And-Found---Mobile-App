package com.example.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
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
    private String currentUserId;

    private static final String DB_URL = "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    public ChatListAdapter(Context context, List<String> userIds) {
        this.context = context;
        this.userIds = userIds;
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("users");
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

        // ---LẤY THÔNG TIN TÊN VÀ AVATAR ---
        usersRef.child(targetUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String avatarBase64 = snapshot.child("avatarUrl").getValue(String.class);

                    if (name != null && !name.isEmpty()) {
                        holder.tvUsername.setText(name);
                    } else if (email != null && !email.isEmpty()) {
                        holder.tvUsername.setText(email);
                    } else {
                        holder.tvUsername.setText("Người dùng (Không tên)");
                    }

                    // Hiển thị Avatar
                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        try {
                            Bitmap bmp = ImageUtil.base64ToBitmap(avatarBase64);
                            if (bmp != null) holder.imgAvatar.setImageBitmap(bmp);
                        } catch (Exception e) {}
                    } else {
                        holder.imgAvatar.setImageResource(R.drawable.ic_notification);
                    }
                } else {
                    holder.tvUsername.setText("Người dùng ẩn danh");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // --- LẤY TIN NHẮN CUỐI CÙNG (Last Message) ---
        loadLastMessage(targetUserId, holder.tvLastMsg);

        // --- SỰ KIỆN CLICK VÀO DÒNG CHAT ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("TARGET_USER_ID", targetUserId);
            context.startActivity(intent);
        });
    }

    private void loadLastMessage(String targetId, TextView tvLastMsg) {
        if (currentUserId == null || targetId == null) return;

        // Tạo ChatID theo quy tắc sắp xếp chữ cái
        String chatId;
        if (currentUserId.compareTo(targetId) < 0) {
            chatId = currentUserId + "_" + targetId;
        } else {
            chatId = targetId + "_" + currentUserId;
        }

        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Chats")
                .child(chatId);

        // Chỉ lấy 1 tin nhắn cuối cùng
        chatRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        String message = data.child("message").getValue(String.class);
                        String senderId = data.child("senderId").getValue(String.class);

                        if (message != null) {
                            if (senderId != null && senderId.equals(currentUserId)) {
                                tvLastMsg.setText("Bạn: " + message);
                            } else {
                                tvLastMsg.setText(message);
                            }
                            tvLastMsg.setTypeface(null, Typeface.NORMAL);
                        }
                    }
                } else {
                    tvLastMsg.setText("Chưa có tin nhắn");
                    tvLastMsg.setTypeface(null, Typeface.ITALIC);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public int getItemCount() { return userIds.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvLastMsg;
        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            // Ánh xạ ID mới thêm bên XML
            tvLastMsg = itemView.findViewById(R.id.tvLastMsg);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}