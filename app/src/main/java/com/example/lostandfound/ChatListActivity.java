package com.example.lostandfound;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    // Link Database chuẩn của bạn
    private static final String DB_URL = "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<String> userList;

    // View thông báo trống và loading
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Setup Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarChatList);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Ánh xạ View
        recyclerView = findViewById(R.id.recyclerChatList);
        layoutEmpty = findViewById(R.id.layoutEmpty); // Sửa: Ánh xạ cả LinearLayout
        progressBar = findViewById(R.id.progressBar); // Sửa: Ánh xạ ProgressBar

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        adapter = new ChatListAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        loadChatList();
    }

    private void loadChatList() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        // Hiện loading trước khi tải
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        // Vào nhánh ChatList > ID của mình
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("ChatList").child(currentUserId);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    // data.getKey() chính là ID của người mình đã chat
                    String otherUserId = data.getKey();
                    if (otherUserId != null) {
                        userList.add(otherUserId);
                    }
                }

                adapter.notifyDataSetChanged();

                // Tắt loading
                progressBar.setVisibility(View.GONE);

                // Kiểm tra danh sách rỗng để hiện thông báo
                if (userList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}