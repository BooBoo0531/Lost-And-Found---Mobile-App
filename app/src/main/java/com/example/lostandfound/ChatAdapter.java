package com.example.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;  // Tin người khác
    public static final int MSG_TYPE_RIGHT = 1; // Tin của mình

    private List<Message> messageList;
    private String currentUserId;

    public ChatAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    // Hàm quyết định vị trí
    @Override
    public int getItemViewType(int position) {
        Message msg = messageList.get(position);
        if (msg.getSenderId().equals(currentUserId)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_left, parent, false);
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message msg = messageList.get(position);

        // Set nội dung tin nhắn
        holder.tvMessage.setText(msg.getMessage());

        // Set thời gian
        if (holder.tvTime != null) {
            // Chuyển timestamp (long) sang giờ phút (String)
            String timeFormatted = formatTime(msg.getTimestamp());
            holder.tvTime.setText(timeFormatted);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        public TextView tvMessage;
        public TextView tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            try {
                tvTime = itemView.findViewById(R.id.tvTime);
            } catch (Exception e) {
                tvTime = null;
            }
        }
    }

    // Format thời gian từ mili-giây sang "HH:mm"
    private String formatTime(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
}