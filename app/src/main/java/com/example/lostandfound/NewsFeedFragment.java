package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NewsFeedFragment extends Fragment {

    // Khai báo các biến View
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;

    // Khai báo biến xử lý dữ liệu
    private PostAdapter postAdapter;
    private List<Post> postList;
    private DatabaseReference databaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news_feed, container, false);

        // 1. Ánh xạ View từ XML (Phải khớp ID với file XML mới)
        recyclerView = view.findViewById(R.id.rvNewsFeed);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        // 2. Cấu hình RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(getContext(), postList);
        recyclerView.setAdapter(postAdapter);

        // 3. Thiết lập trạng thái ban đầu (Đang tải...)
        progressBar.setVisibility(View.VISIBLE);      // Hiện vòng xoay
        recyclerView.setVisibility(View.GONE);        // Ẩn danh sách
        layoutEmptyState.setVisibility(View.GONE);    // Ẩn thông báo rỗng

        // 4. Kết nối Firebase
        try {
            // Đảm bảo link này đúng với database của bạn
            databaseReference = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("posts");
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // 5. Gọi hàm lấy dữ liệu
        fetchPosts();

        return view;
    }

    private void fetchPosts() {
        if (databaseReference == null) return;

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                // Duyệt qua tất cả các bài viết trên Firebase
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    if (post != null) {
                        // add(0, post) để đưa bài mới nhất lên đầu danh sách
                        postList.add(0, post);
                    }
                }
                postAdapter.notifyDataSetChanged();

                // --- QUAN TRỌNG: CẬP NHẬT GIAO DIỆN ---
                progressBar.setVisibility(View.GONE); // Tắt vòng xoay loading

                if (postList.isEmpty()) {
                    // Nếu danh sách rỗng -> Hiện thông báo "Chưa có bài nào"
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    // Nếu có bài -> Hiện danh sách lên
                    layoutEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý khi lỗi mạng hoặc lỗi server
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi tải tin: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}