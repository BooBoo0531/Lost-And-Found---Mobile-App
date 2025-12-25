package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;

    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    private DatabaseReference databaseReference;
    private ValueEventListener postsListener;

    private SharedPostViewModel postVM;

    private boolean hasLoadedOnce = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_news_feed, container, false);

        recyclerView = view.findViewById(R.id.rvNewsFeed);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(getContext(), postList);
        recyclerView.setAdapter(postAdapter);

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);

        postVM = new ViewModelProvider(requireActivity()).get(SharedPostViewModel.class);
        postVM.getPosts().observe(getViewLifecycleOwner(), posts -> {
            postList.clear();
            if (posts != null) postList.addAll(posts);
            postAdapter.notifyDataSetChanged();

            if (hasLoadedOnce) {
                updateUiState();
            }
        });

        try {
            databaseReference = FirebaseDatabase
                    .getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("posts");
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        fetchPosts();

        return view;
    }

    private void fetchPosts() {
        if (databaseReference == null) return;

        if (postsListener != null) {
            databaseReference.removeEventListener(postsListener);
        }

        postsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Post> latest = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    if (post != null) {
                        if (post.getId() == null || post.getId().trim().isEmpty()) {
                            post.setId(dataSnapshot.getKey());
                        }
                        latest.add(0, post);
                    }
                }

                hasLoadedOnce = true;

                postVM.setPosts(latest);

                progressBar.setVisibility(View.GONE);
                updateUiState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hasLoadedOnce = true;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi tải tin: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                updateUiState();
            }
        };

        databaseReference.addValueEventListener(postsListener);
    }

    private void updateUiState() {
        if (postList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseReference != null && postsListener != null) {
            databaseReference.removeEventListener(postsListener);
        }
        postsListener = null;
    }
}
