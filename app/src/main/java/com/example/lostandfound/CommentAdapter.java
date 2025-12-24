package com.example.lostandfound;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {

    public interface OnReplyClick {
        void onReply(Comment c);
    }

    private final Context context;
    private final String postId;
    private final List<Comment> list;
    private final DatabaseReference usersRef;
    private final DatabaseReference repliesRootRef; // replies/{postId}/{commentId}
    private final OnReplyClick onReplyClick;

    private final Set<String> expanded = new HashSet<>();

    public CommentAdapter(Context context,
                          String postId,
                          List<Comment> list,
                          DatabaseReference usersRef,
                          DatabaseReference repliesRootRef,
                          OnReplyClick onReplyClick) {
        this.context = context;
        this.postId = postId;
        this.list = list;
        this.usersRef = usersRef;
        this.repliesRootRef = repliesRootRef;
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

        // reset state (tránh recycle sai)
        h.tvViewReplies.setVisibility(View.GONE);
        h.rvReplies.setVisibility(View.GONE);
        h.tvViewReplies.setOnClickListener(null);

        // username bold + content
        String name = safe(c.userEmail);
        String msg = pickMessage(c);
        if ((msg == null || msg.trim().isEmpty()) && c.imageBase64 != null && !c.imageBase64.isEmpty()) {
            msg = "🖼 Ảnh";
        }

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(name).append("  ");
        if (!name.isEmpty()) {
            sb.setSpan(new StyleSpan(Typeface.BOLD),
                    0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        sb.append(safe(msg));
        h.tvContent.setText(sb);

        // time
        String time = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(new Date(c.timestamp));
        h.tvTime.setText(time);

        // ảnh comment
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

        // reply click
        h.btnReply.setOnClickListener(v -> {
            if (onReplyClick != null) onReplyClick.onReply(c);
        });

        // avatar (✅ không padding, không tint, không filter)
        bindAvatar(h, c.userId);

        // ===== replies nested =====
        String commentId = (c.id == null) ? "" : c.id.trim();
        if (commentId.isEmpty() || repliesRootRef == null || postId == null || postId.trim().isEmpty()) return;

        h.tvViewReplies.setTag(commentId);
        h.rvReplies.setTag(commentId);

        DatabaseReference thisRepliesRef = repliesRootRef.child(postId).child(commentId);

        thisRepliesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object tag1 = h.tvViewReplies.getTag();
                Object tag2 = h.rvReplies.getTag();
                if (tag1 == null || tag2 == null) return;
                if (!commentId.equals(tag1.toString()) || !commentId.equals(tag2.toString())) return;

                int count = (int) snapshot.getChildrenCount();
                if (count <= 0) {
                    h.tvViewReplies.setVisibility(View.GONE);
                    h.rvReplies.setVisibility(View.GONE);
                    return;
                }

                boolean isExpanded = expanded.contains(commentId);

                if (!isExpanded) {
                    h.tvViewReplies.setText("Xem " + count + " câu trả lời khác");
                    h.tvViewReplies.setVisibility(View.VISIBLE);
                    h.rvReplies.setVisibility(View.GONE);
                } else {
                    h.tvViewReplies.setText("Ẩn câu trả lời");
                    h.tvViewReplies.setVisibility(View.VISIBLE);
                    h.rvReplies.setVisibility(View.VISIBLE);
                    bindRepliesRecycler(h, thisRepliesRef);
                }

                h.tvViewReplies.setOnClickListener(v -> {
                    if (expanded.contains(commentId)) expanded.remove(commentId);
                    else expanded.add(commentId);
                    notifyItemChanged(position);
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindRepliesRecycler(@NonNull VH h, @NonNull DatabaseReference thisRepliesRef) {
        if (h.rvReplies.getLayoutManager() == null) {
            h.rvReplies.setLayoutManager(new LinearLayoutManager(context));
            h.rvReplies.setNestedScrollingEnabled(false);

            // thụt vào giống IG
            int pad = dp(12);
            h.rvReplies.setPadding(pad, 0, 0, 0);
            h.rvReplies.setClipToPadding(false);
        }

        List<Reply> replyList = new ArrayList<>();
        ReplyAdapter replyAdapter = new ReplyAdapter(replyList);
        h.rvReplies.setAdapter(replyAdapter);

        thisRepliesRef.orderByChild("createdAt")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object tag = h.rvReplies.getTag();
                        if (tag == null) return;

                        replyList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Reply r = child.getValue(Reply.class);
                            if (r == null) continue;
                            if (r.id == null || r.id.trim().isEmpty()) r.id = child.getKey();
                            replyList.add(r);
                        }
                        replyAdapter.notifyDataSetChanged();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String pickMessage(Comment c) {
        if (c == null) return "";
        if (c.content != null && !c.content.trim().isEmpty()) return c.content;
        if (c.text != null && !c.text.trim().isEmpty()) return c.text;
        return "";
    }

    // ✅ avatar: không padding, không colorFilter, không tint
    private void bindAvatar(@NonNull VH h, String userId) {
        h.imgAvatar.setTag(userId);

        h.imgAvatar.setImageResource(R.drawable.ic_notification);
        h.imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        h.imgAvatar.setPadding(0, 0, 0, 0);
        h.imgAvatar.clearColorFilter();
        h.imgAvatar.setImageTintList(null);

        if (userId == null || userId.trim().isEmpty() || usersRef == null) return;

        usersRef.child(userId).child("avatarUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object tag = h.imgAvatar.getTag();
                        if (tag == null || !userId.equals(tag.toString())) return;

                        String base64 = snapshot.getValue(String.class);
                        if (base64 == null || base64.isEmpty()) return;

                        Bitmap bmp = ImageUtil.base64ToBitmap(base64);
                        if (bmp != null) {
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
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        ImageView imgAvatar, imgComment;
        TextView tvContent, tvTime, btnReply;
        TextView tvViewReplies;
        RecyclerView rvReplies;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.rootComment);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            imgComment = itemView.findViewById(R.id.imgComment);
            btnReply = itemView.findViewById(R.id.btnReply);
            tvViewReplies = itemView.findViewById(R.id.tvViewReplies);
            rvReplies = itemView.findViewById(R.id.rvReplies);
        }
    }
}
