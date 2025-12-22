package com.example.lostandfound;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
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
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnItemClick {
        void onClick(NotificationItem item);
    }

    private final Context context;
    private final List<NotificationItem> list;
    private final DatabaseReference usersRef;
    private final OnItemClick onItemClick;

    private final Map<String, UserCache> cache = new HashMap<>();

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
        NotificationItem item = list.get(position);

        // default
        String email = (item.fromEmail != null && !item.fromEmail.trim().isEmpty())
                ? item.fromEmail
                : "Ai đó";
        h.tvName.setText(email);

        String msg = (item.content == null) ? "" : item.content.trim();
        h.tvContent.setText(msg);

        String time = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(new Date(item.timestamp));
        h.tvTime.setText(time);

        boolean unread = !item.isRead;
        h.tvUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        h.tvName.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);

        h.imgAvatar.setImageResource(R.drawable.ic_notification);
        h.imgAvatar.setPadding(dp(6), dp(6), dp(6), dp(6));
        h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        bindUser(h, item);

        h.root.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(item);
        });
    }

    private void bindUser(@NonNull VH h, @NonNull NotificationItem item) {

        String email = (item.fromEmail != null && !item.fromEmail.trim().isEmpty())
                ? item.fromEmail.trim()
                : "";
        if (!email.isEmpty()) h.tvName.setText(email);

        h.imgAvatar.setImageResource(R.drawable.ic_notification);
        h.imgAvatar.setPadding(dp(6), dp(6), dp(6), dp(6));
        h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        String uid = item.fromUserId;
        if (uid != null && !uid.trim().isEmpty() && usersRef != null) {
            h.imgAvatar.setTag("uid:" + uid);

            usersRef.child(uid).child("avatarUrl")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Object tag = h.imgAvatar.getTag();
                            if (tag == null || !("uid:" + uid).equals(tag.toString())) return;

                            String base64 = snapshot.getValue(String.class);
                            if (base64 == null || base64.isEmpty()) return;

                            Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                            if (bmp != null) {
                                h.imgAvatar.setPadding(0, 0, 0, 0);
                                h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                h.imgAvatar.setImageBitmap(bmp);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });

            return;
        }

        if (usersRef == null || email.isEmpty()) return;

        h.imgAvatar.setTag("email:" + email);

        usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object tag = h.imgAvatar.getTag();
                        if (tag == null || !("email:" + email).equals(tag.toString())) return;

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String avatar = userSnap.child("avatarUrl").getValue(String.class);
                            if (avatar == null || avatar.isEmpty()) continue;

                            Bitmap bmp = ImageUtil.base64ToBitmap(avatar);
                            if (bmp != null) {
                                h.imgAvatar.setPadding(0, 0, 0, 0);
                                h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                h.imgAvatar.setImageBitmap(bmp);
                            }
                            break;
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


    private String firstNonEmpty(String... arr) {
        if (arr == null) return "";
        for (String s : arr) {
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        return "";
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
        ImageView imgAvatar;
        TextView tvName, tvContent, tvTime, tvUnreadDot;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.rootNotify);
            imgAvatar = itemView.findViewById(R.id.imgNotifyAvatar);
            tvName = itemView.findViewById(R.id.tvNotifyName);
            tvContent = itemView.findViewById(R.id.tvNotifyContent);
            tvTime = itemView.findViewById(R.id.tvNotifyTime);
            tvUnreadDot = itemView.findViewById(R.id.tvUnreadDot);
        }
    }

    static class UserCache {
        String name;
        String avatarBase64;
        UserCache(String name, String avatarBase64) {
            this.name = name == null ? "" : name;
            this.avatarBase64 = avatarBase64 == null ? "" : avatarBase64;
        }
    }
}
