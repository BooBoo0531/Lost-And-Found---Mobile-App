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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.VH> {

    // Interface để báo cho CommentAdapter biết khi bấm trả lời
    public interface OnReplyClickListener {
        void onReplyClick(String parentCommentId, Reply reply);
    }

    private final List<Reply> list;
    private final String parentCommentId;
    private final OnReplyClickListener listener; // Thêm listener
    private final DatabaseReference usersRef;

    // ✅ Constructor mới nhận 3 tham số để khớp với CommentAdapter
    public ReplyAdapter(List<Reply> list, String parentCommentId, OnReplyClickListener listener) {
        this.list = list;
        this.parentCommentId = parentCommentId;
        this.listener = listener;
        this.usersRef = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users");
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Reply r = list.get(position);

        // 1. XỬ LÝ TÊN (Cắt bỏ @gmail.com)
        String name = r.userEmail != null ? r.userEmail : "Ẩn danh";
        if (name.contains("@")) {
            name = name.substring(0, name.indexOf("@"));
        }
        h.tvName.setText(name);

        // 2. HIỂN THỊ NỘI DUNG
        h.tvText.setText(r.text != null ? r.text : "");

        // 3. HIỂN THỊ GIỜ
        if (r.createdAt > 0) {
            h.tvTime.setText(new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(r.createdAt)));
        }

        // 4. NÚT TRẢ LỜI
        h.tvReplyBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReplyClick(parentCommentId, r);
            }
        });

        // 5. LOAD AVATAR & ẢNH REPLY
        bindAvatar(h.imgAvatar, r.userId);

        if (r.imageBase64 != null && !r.imageBase64.isEmpty()) {
            h.imgReply.setVisibility(View.VISIBLE);
            Bitmap bmp = ImageUtil.base64ToBitmap(r.imageBase64);
            if (bmp != null) h.imgReply.setImageBitmap(bmp);
        } else {
            h.imgReply.setVisibility(View.GONE);
        }
    }

    private void bindAvatar(ImageView img, String uid) {
        // --- TRẠNG THÁI MẶC ĐỊNH (Khi chưa có ảnh) ---
        img.setImageResource(R.drawable.ic_notification); // Hiện icon thông báo
        int pad = dp(img.getContext(), 6); // Tính padding 6dp
        img.setPadding(pad, pad, pad, pad); // Set padding để icon nhỏ lại, nằm giữa
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Canh giữa

        if (uid == null || uid.isEmpty()) return;

        usersRef.child(uid).child("avatarUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String b64 = snapshot.getValue(String.class);
                if (b64 != null && !b64.isEmpty()) {
                    Bitmap bmp = ImageUtil.base64ToBitmap(b64);
                    if (bmp != null) {
                        // --- KHI LOAD ĐƯỢC ẢNH THẬT ---
                        img.setPadding(0, 0, 0, 0); // ⚠️ QUAN TRỌNG: Xóa padding đi
                        img.setScaleType(ImageView.ScaleType.CENTER_CROP); // Crop ảnh cho đầy khung tròn
                        img.setImageBitmap(bmp); // Hiện ảnh
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgReply;
        TextView tvName, tvText, tvTime, tvReplyBtn;

        VH(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgAvatarReply);
            imgReply = v.findViewById(R.id.imgReply);
            tvName = v.findViewById(R.id.tvNameReply);
            tvText = v.findViewById(R.id.tvTextReply);
            tvTime = v.findViewById(R.id.tvTimeReply);
            // Bạn cần thêm ID này vào item_reply.xml nếu chưa có: android:id="@+id/btnReplySmall"
            tvReplyBtn = v.findViewById(R.id.btnReplySmall);
        }
    }
    private int dp(Context context, int v) {
        float d = context.getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }
}