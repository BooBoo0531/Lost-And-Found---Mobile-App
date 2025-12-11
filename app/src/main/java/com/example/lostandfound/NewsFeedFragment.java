package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private DatabaseReference databaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news_feed, container, false);

        // SỬA LỖI 3: Đảm bảo ID này khớp với file fragment_news_feed.xml
        // Nếu trong XML là @+id/recyclerViewPosts thì ở đây phải là recyclerViewPosts
        recyclerView = view.findViewById(R.id.rvNewsFeed);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        postList = new ArrayList<>();
        // Constructor này giờ đã khớp với bên Adapter
        postAdapter = new PostAdapter(getContext(), postList);
        recyclerView.setAdapter(postAdapter);

        try {
            databaseReference = FirebaseDatabase.getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("posts");
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        fetchPosts();

        return view;
    }

    private void fetchPosts() {
        if (databaseReference == null) return;

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    if (post != null) {
                        postList.add(0, post);
                    }
                }
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi tải tin", Toast.LENGTH_SHORT).show();
            }
        });
    }
}