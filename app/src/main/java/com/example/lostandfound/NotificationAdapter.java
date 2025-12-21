package com.example.lostandfound;

import android.content.Context;
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
    private final OnItemClick onItemClick;

    public NotificationAdapter(Context context,
                               List<NotificationItem> list,
                               DatabaseReference usersRef,
                               OnItemClick onItemClick) {
        this.context = context;
        this.list = list;
        this.usersRef = usersRef;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationItem n = list.get(position);

        String who = (n.fromEmail == null || n.fromEmail.isEmpty()) ? "Ai đó" : n.fromEmail;

        String title;
        if ("REPLY".equalsIgnoreCase(n.type)) {
            title = who + " đã trả lời bình luận của bạn";
        } else {
            title = who + " đã bình luận bài viết của bạn";
        }
        h.tvTitle.setText(title);

        h.tvContent.setText(n.content == null ? "" : n.content);

        String time = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(new Date(n.timestamp));
        h.tvTime.setText(time);

        h.dotUnread.setVisibility(n.isRead ? View.GONE : View.VISIBLE);

        // avatar
        bindAvatar(h, n.fromUserId);

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(n);
        });
    }

    private void bindAvatar(@NonNull VH h, String fromUserId) {
        h.imgAvatar.setImageResource(R.drawable.ic_notification);
        h.imgAvatar.setPadding(dp(6), dp(6), dp(6), dp(6));
        h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        if (fromUserId == null || fromUserId.trim().isEmpty() || usersRef == null) return;

        h.imgAvatar.setTag(fromUserId);
        usersRef.child(fromUserId).child("avatarUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object tag = h.imgAvatar.getTag();
                        if (tag == null || !fromUserId.equals(tag.toString())) return;

                        String base64 = snapshot.getValue(String.class);
                        if (base64 == null || base64.isEmpty()) return;

                        Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                        if (bmp != null) {
                            h.imgAvatar.setPadding(0,0,0,0);
                            h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            h.imgAvatar.setImageBitmap(bmp);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
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
        ImageView imgAvatar;
        View dotUnread;
        TextView tvTitle, tvContent, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgNotifyAvatar);
            dotUnread = itemView.findViewById(R.id.dotUnread);
            tvTitle = itemView.findViewById(R.id.tvNotifyTitle);
            tvContent = itemView.findViewById(R.id.tvNotifyContent);
            tvTime = itemView.findViewById(R.id.tvNotifyTime);
        }
    }
}
