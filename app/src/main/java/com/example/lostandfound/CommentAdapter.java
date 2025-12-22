package com.example.lostandfound;

import android.content.Context;
import android.graphics.Bitmap;
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
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {

    public interface OnReplyClick {
        void onReply(Comment c);
    }

    private final Context context;
    private final List<Comment> list;
    private final DatabaseReference usersRef;
    private final OnReplyClick onReplyClick;

    public CommentAdapter(Context context, List<Comment> list, DatabaseReference usersRef, OnReplyClick onReplyClick) {
        this.context = context;
        this.list = list;
        this.usersRef = usersRef;
        this.onReplyClick = onReplyClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Comment c = list.get(position);

        h.tvName.setText(safe(c.userEmail));

        String msg = pickMessage(c);
        if ((msg == null || msg.trim().isEmpty()) && c.imageBase64 != null && !c.imageBase64.isEmpty()) {
            msg = "🖼 Ảnh";
        }
        h.tvContent.setText(safe(msg));

        String time = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(new Date(c.timestamp));
        h.tvTime.setText(time);

        boolean isReply = c.parentId != null && !c.parentId.trim().isEmpty();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) h.root.getLayoutParams();
        if (lp != null) {
            lp.leftMargin = isReply ? dp(46) : dp(0);
            h.root.setLayoutParams(lp);
        }

        if (c.imageBase64 != null && !c.imageBase64.isEmpty()) {
            Bitmap bmp = ImageUtil.base64ToBitmap(c.imageBase64);
            if (bmp != null) {
                h.imgComment.setVisibility(View.VISIBLE);
                h.imgComment.setImageBitmap(bmp);
            } else {
                h.imgComment.setVisibility(View.GONE);
            }
        } else {
            h.imgComment.setVisibility(View.GONE);
        }

        h.btnReply.setOnClickListener(v -> {
            if (onReplyClick != null) onReplyClick.onReply(c);
        });

        bindAvatar(h, c.userId);
    }

    private String pickMessage(Comment c) {
        if (c == null) return "";
        if (c.content != null && !c.content.trim().isEmpty()) return c.content;
        if (c.text != null && !c.text.trim().isEmpty()) return c.text;
        return "";
    }

    private void bindAvatar(@NonNull VH h, String userId) {
        h.imgAvatar.setImageResource(R.drawable.ic_notification);
        h.imgAvatar.setPadding(dp(6), dp(6), dp(6), dp(6));

        if (userId == null || userId.trim().isEmpty() || usersRef == null) return;

        h.imgAvatar.setTag(userId);
        usersRef.child(userId).child("avatarUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object tag = h.imgAvatar.getTag();
                        if (tag == null || !userId.equals(tag.toString())) return;

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

    private String safe(String s) { return s == null ? "" : s; }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        ImageView imgAvatar, imgComment;
        TextView tvName, tvTime, tvContent, btnReply;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.rootComment);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            imgComment = itemView.findViewById(R.id.imgComment);
            btnReply = itemView.findViewById(R.id.btnReply);
        }
    }
}
