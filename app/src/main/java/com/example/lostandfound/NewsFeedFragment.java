package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NewsFeedFragment extends Fragment {

    private RecyclerView rvNewsFeed;
    private PostAdapter postAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news_feed, container, false);

        // 1. Ánh xạ RecyclerView
        rvNewsFeed = view.findViewById(R.id.rvNewsFeed);

        // 2. Cấu hình RecyclerView
        rvNewsFeed.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Khởi tạo Adapter (Tái sử dụng PostAdapter cũ)
        postAdapter = new PostAdapter();
        rvNewsFeed.setAdapter(postAdapter);

        // 4. Load dữ liệu giả (Mô phỏng lấy tất cả bài từ API)
        loadAllPosts();

        return view;
    }

    private void loadAllPosts() {
        List<Post> allPosts = new ArrayList<>();

        // Giả lập dữ liệu hỗn hợp từ nhiều nơi
        allPosts.add(new Post("Nguyễn Văn A", "5 phút trước", "Cần tìm ví rơi ở Quận 1, TP.HCM. Trong ví có CCCD...", "LOST"));
        allPosts.add(new Post("Trần Thị B", "30 phút trước", "Nhặt được chìa khóa xe máy tại Cầu Giấy, Hà Nội", "FOUND"));
        allPosts.add(new Post("Lê Văn C", "1 giờ trước", "Tìm chó Poodle lạc ở Đà Nẵng, xin hậu tạ!", "LOST"));
        allPosts.add(new Post("Phạm D", "2 giờ trước", "Ai nhặt được thẻ sinh viên tên Phạm D ở Thủ Đức k ạ?", "LOST"));
        allPosts.add(new Post("Hoàng E", "1 ngày trước", "Nhặt được túi xách màu đen ở quán Cafe...", "FOUND"));

        // Đưa dữ liệu vào Adapter
        postAdapter.setPostList(allPosts);
    }
}