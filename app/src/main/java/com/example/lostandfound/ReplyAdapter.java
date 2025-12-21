package com.example.lostandfound;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.VH> {

    private final List<Reply> list;
    private final DatabaseReference usersRef;

    public ReplyAdapter(List<Reply> list) {
        this.list = list;
        usersRef = FirebaseDatabase
                .getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users");
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Reply r = list.get(pos);
        h.tvName.setText(safe(r.userEmail));
        h.tvText.setText(safe(r.text));
        h.tvTime.setText(formatTime(r.createdAt));

        bindAvatar(h.imgAvatar, r.userId);

        if (r.imageBase64 != null && !r.imageBase64.isEmpty()) {
            h.imgReply.setVisibility(View.VISIBLE);
            Bitmap bmp = ImageUtil.base64ToBitmap(r.imageBase64);
            if (bmp != null) h.imgReply.setImageBitmap(bmp);
            else h.imgReply.setVisibility(View.GONE);
        } else {
            h.imgReply.setVisibility(View.GONE);
        }
    }

    private void bindAvatar(ImageView img, String uid) {
        img.setImageResource(R.drawable.ic_notification);
        img.setPadding(8,8,8,8);
        img.setColorFilter(0xFF888888);
        img.setTag(uid);

        if (uid == null || uid.isEmpty()) return;

        usersRef.child(uid).child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object tag = img.getTag();
                if (tag == null || !uid.equals(tag.toString())) return;

                String b64 = snapshot.getValue(String.class);
                if (b64 == null || b64.isEmpty()) return;

                Bitmap bmp = ImageUtil.base64ToBitmap(b64);
                if (bmp != null) {
                    img.clearColorFilter();
                    img.setPadding(0,0,0,0);
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setImageBitmap(bmp);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String safe(String s){ return s == null ? "" : s; }

    private String formatTime(long ms){
        if (ms <= 0) return "";
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(ms);
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgReply;
        TextView tvName, tvText, tvTime;
        VH(@NonNull View v){
            super(v);
            imgAvatar = v.findViewById(R.id.imgAvatarReply);
            imgReply = v.findViewById(R.id.imgReply);
            tvName = v.findViewById(R.id.tvNameReply);
            tvText = v.findViewById(R.id.tvTextReply);
            tvTime = v.findViewById(R.id.tvTimeReply);
        }
    }
}
