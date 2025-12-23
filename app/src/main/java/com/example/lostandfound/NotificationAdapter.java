package com.example.lostandfound;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnItemClick {
        void onClick(NotificationItem item);
    }

    private final Context context;
    private final List<NotificationItem> list;
    private final DatabaseReference usersRef;
    private final DatabaseReference postsRef;
    private final OnItemClick onItemClick;

    public NotificationAdapter(Context context,
                               List<NotificationItem> list,
                               DatabaseReference usersRef,
                               OnItemClick onItemClick) {
        this.context = context;
        this.list = list;
        this.usersRef = usersRef;
        this.onItemClick = onItemClick;
        // Reference đến bảng posts để lấy ảnh thumbnail
        this.postsRef = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("posts");
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationItem item = list.get(position);

        // Xử lý tên người gửi
        String rawName = (item.fromEmail != null && !item.fromEmail.trim().isEmpty()) ? item.fromEmail : "Người dùng ẩn danh";
        String displayName = rawName.contains("@") ? rawName.substring(0, rawName.indexOf("@")) : rawName;

        // Xử lý Câu thông báo dựa trên TYPE
        String type = item.type != null ? item.type : "";
        String htmlTitle;

        switch (type.toUpperCase()) {
            case "COMMENT":
                htmlTitle = "<b>" + displayName + "</b> đã bình luận về bài viết của bạn";
                break;
            case "REPLY":
                htmlTitle = "<b>" + displayName + "</b> đã trả lời bình luận của bạn";
                break;
            case "FOUND":
                htmlTitle = "<b>" + displayName + "</b> báo rằng đã tìm thấy món đồ của bạn";
                break;
            default:
                htmlTitle = "<b>" + displayName + "</b>";
                break;
        }

        h.tvName.setText(Html.fromHtml(htmlTitle, Html.FROM_HTML_MODE_LEGACY));

        h.tvContent.setText(item.content != null ? item.content.trim() : "");

        String time = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(item.timestamp));
        h.tvTime.setText(time);

        boolean unread = !item.isRead;
        h.dotUnread.setVisibility(unread ? View.VISIBLE : View.GONE);

        if (unread) {
            h.root.setBackgroundColor(0xFFF0F8FF); // Xanh nhạt
        } else {
            h.root.setBackgroundColor(0xFFFFFFFF); // Trắng
        }

        bindUserAvatar(h, item);
        bindPostThumbnail(h, item);

        h.root.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(item);
        });
    }

    // --- HÀM LOAD AVATAR ---
    private void bindUserAvatar(@NonNull VH h, @NonNull NotificationItem item) {
        h.imgAvatar.setImageResource(R.drawable.ic_notification);
        h.imgAvatar.setPadding(dp(10), dp(10), dp(10), dp(10));
        h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        h.imgAvatar.setColorFilter(0xFF888888);

        String uid = item.fromUserId;
        if (uid != null && !uid.trim().isEmpty() && usersRef != null) {
            h.imgAvatar.setTag(uid);
            usersRef.child(uid).child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!uid.equals(h.imgAvatar.getTag())) return;
                    String base64 = snapshot.getValue(String.class);
                    if (base64 != null && !base64.isEmpty()) {
                        Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                        if (bmp != null) {
                            h.imgAvatar.clearColorFilter();
                            h.imgAvatar.setPadding(0, 0, 0, 0);
                            h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            h.imgAvatar.setImageBitmap(bmp);
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    // --- HÀM LOAD THUMBNAIL BÀI VIẾT ---
    private void bindPostThumbnail(@NonNull VH h, @NonNull NotificationItem item) {
        h.imgPostThumbnail.setVisibility(View.GONE);
        h.imgPostThumbnail.setImageBitmap(null);

        String postId = item.postId;
        if (postId != null && !postId.isEmpty() && postsRef != null) {
            h.imgPostThumbnail.setTag(postId);
            postsRef.child(postId).child("imageBase64").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!postId.equals(h.imgPostThumbnail.getTag())) return;
                    String base64 = snapshot.getValue(String.class);
                    if (base64 != null && !base64.isEmpty()) {
                        Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                        if (bmp != null) {
                            h.imgPostThumbnail.setVisibility(View.VISIBLE);
                            h.imgPostThumbnail.setImageBitmap(bmp);
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private int dp(int v) {
        float d = context.getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        ImageView imgAvatar, imgPostThumbnail;
        TextView tvName, tvContent, tvTime;
        View dotUnread;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.rootNotify);
            imgAvatar = itemView.findViewById(R.id.imgNotifyAvatar);
            imgPostThumbnail = itemView.findViewById(R.id.imgPostThumbnail);
            tvName = itemView.findViewById(R.id.tvNotifyName);
            tvContent = itemView.findViewById(R.id.tvNotifyContent);
            tvTime = itemView.findViewById(R.id.tvNotifyTime);
            dotUnread = itemView.findViewById(R.id.dotUnread);
        }
    }
}