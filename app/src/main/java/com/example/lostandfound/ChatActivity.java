package com.example.lostandfound;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText edtMessageInput;
    private ImageButton btnSend;

    private ChatAdapter chatAdapter;
    private List<Message> messageList;

    private DatabaseReference mRef;
    private String currentUserId;
    private String targetUserId;
    private String chatId;
    private static final String DB_URL = "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        targetUserId = getIntent().getStringExtra("TARGET_USER_ID");
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Kiểm tra an toàn: Nếu không có User ID thì thoát để tránh lỗi
        if (currentUserId == null || targetUserId == null) {
            Toast.makeText(this, "Lỗi xác định người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Tạo Chat ID duy nhất
        if (currentUserId.compareTo(targetUserId) < 0) {
            chatId = currentUserId + "_" + targetUserId;
        } else {
            chatId = targetUserId + "_" + currentUserId;
        }

        try {
            mRef = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Chats")
                    .child(chatId);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        readMessages();

        // Sự kiện gửi tin nhắn
        btnSend.setOnClickListener(v -> {
            String txt = edtMessageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(txt)) {
                sendMessage(txt);
            }
        });
    }

    private void initViews() {
        recyclerChat = findViewById(R.id.recyclerChat);
        edtMessageInput = findViewById(R.id.edtMessageInput);
        btnSend = findViewById(R.id.btnSend);

        messageList = new ArrayList<>();
        // Truyền currentUserId vào Adapter để phân biệt tin nhắn trái/phải
        chatAdapter = new ChatAdapter(messageList, currentUserId);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true); // Luôn hiển thị tin nhắn mới nhất ở dưới cùng
        recyclerChat.setLayoutManager(linearLayoutManager);
        recyclerChat.setAdapter(chatAdapter);
    }

    private void sendMessage(String text) {
        long timestamp = System.currentTimeMillis();
        Message message = new Message(currentUserId, text, timestamp);

        // 1. Lưu tin nhắn vào nhánh Chats (Code cũ)
        if (mRef != null) {
            mRef.push().setValue(message);
        }

        // CẬP NHẬT DANH SÁCH CHAT
        DatabaseReference chatListRef = FirebaseDatabase.getInstance(DB_URL).getReference("ChatList");

        chatListRef.child(currentUserId).child(targetUserId).child("id").setValue(targetUserId);

        chatListRef.child(targetUserId).child(currentUserId).child("id").setValue(currentUserId);

        // GỬI THÔNG BÁO (Notification)
        sendNotification(targetUserId, text);

        edtMessageInput.setText("");
    }

    // Hàm đẩy thông báo
    private void sendNotification(String receiverId, String content) {
        DatabaseReference notifyRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("notifications")
                .child(receiverId);

        String notiId = notifyRef.push().getKey();
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Tạo đối tượng NotificationItem (dùng chung class với phần Comment)
        NotificationItem noti = new NotificationItem();
        noti.id = notiId;
        noti.toUserId = receiverId;
        noti.fromUserId = currentUserId;
        noti.fromEmail = myEmail;
        noti.postId = "";
        noti.type = "MESSAGE";
        noti.content = content;
        noti.timestamp = System.currentTimeMillis();
        noti.isRead = false;

        if (notiId != null) {
            notifyRef.child(notiId).setValue(noti);
        }
    }

    private void readMessages() {
        if (mRef == null) return;

        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Message msg = data.getValue(Message.class);
                    if (msg != null) {
                        messageList.add(msg);
                    }
                }
                chatAdapter.notifyDataSetChanged();

                if (!messageList.isEmpty()) {
                    recyclerChat.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi nếu cần
            }
        });
    }
}