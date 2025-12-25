package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmpty, tvGoNews;

    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    private DatabaseReference postsRef;
    private ValueEventListener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        progressBar = view.findViewById(R.id.progressBarHistory);
        layoutEmpty = view.findViewById(R.id.layoutEmptyHistory);
        tvEmpty = view.findViewById(R.id.tvEmptyHistory);
        tvGoNews = view.findViewById(R.id.tvGoNewsFeed);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(requireContext(), postList);
        rvHistory.setAdapter(postAdapter);

        showLoading();

        try {
            postsRef = FirebaseDatabase
                    .getInstance("https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("posts");
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi kết nối DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showEmpty("Không kết nối được database.\nBấm để xem bài đăng.");
            return view;
        }

        tvGoNews.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NewsFeedFragment())
                    .commit();
        });

        fetchMyPosts();
        return view;
    }

    private void fetchMyPosts() {
        if (postsRef == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : null;
        String email = (user != null) ? user.getEmail() : null;

        if ((uid == null || uid.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
            showEmpty("Bạn chưa đăng bài viết nào.\n(Đăng nhập để xem lịch sử bài đã đăng)");
            return;
        }

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Post p = child.getValue(Post.class);
                    if (p == null) continue;

                    if (p.getId() == null || p.getId().trim().isEmpty()) {
                        p.setId(child.getKey());
                    }

                    if (isMine(p, uid, email)) {
                        postList.add(0, p);
                    }
                }

                postAdapter.notifyDataSetChanged();

                if (postList.isEmpty()) {
                    showEmpty("Bạn chưa đăng bài viết nào.");
                } else {
                    showList();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi tải lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty("Không tải được lịch sử.\nBấm để xem bài đăng.");
            }
        };

        postsRef.addValueEventListener(listener);
    }

    private boolean isMine(@NonNull Post p, @Nullable String uid, @Nullable String email) {
        // ưu tiên theo uid
        try {
            String postUid = p.getUserId();
            if (uid != null && !uid.trim().isEmpty()
                    && postUid != null && uid.equals(postUid)) {
                return true;
            }
        } catch (Exception ignored) {}

        try {
            String postEmail = p.getUserEmail();
            if (email != null && !email.trim().isEmpty()
                    && postEmail != null && email.equalsIgnoreCase(postEmail)) {
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvHistory.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showList() {
        progressBar.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvHistory.setVisibility(View.VISIBLE);
    }

    private void showEmpty(String message) {
        progressBar.setVisibility(View.GONE);
        rvHistory.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postsRef != null && listener != null) {
            postsRef.removeEventListener(listener);
        }
    }
}
