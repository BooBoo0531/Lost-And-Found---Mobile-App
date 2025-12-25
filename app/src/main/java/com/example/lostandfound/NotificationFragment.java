package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Collections;
import java.util.List;



public class NotificationFragment extends Fragment {

    private static final String DB_URL =
            "https://lostandfound-4930e-default-rtdb.asia-southeast1.firebasedatabase.app";

    private RecyclerView rv;
    private TextView tvEmpty;

    private final List<NotificationItem> list = new ArrayList<>();
    private NotificationAdapter adapter;

    private DatabaseReference notifyRef;
    private DatabaseReference usersRef;

    private ValueEventListener notifyListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_notification, container, false);

        rv = v.findViewById(R.id.rvNotify);
        tvEmpty = v.findViewById(R.id.tvEmptyNotify);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Bạn chưa đăng nhập");
            return v;
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        notifyRef = db.getReference("notifications").child(u.getUid());
        usersRef = db.getReference("users");

        adapter = new NotificationAdapter(requireContext(), list, usersRef, item -> {
            // mark read
            if (notifyRef != null && item.id != null && !item.id.isEmpty()) {
                notifyRef.child(item.id).child("isRead").setValue(true);
            }

            try {
                NotificationDetailBottomSheetDialogFragment.newInstance(item)
                        .show(requireActivity().getSupportFragmentManager(), "NOTI_DETAIL");
            } catch (Exception e) {
                Toast.makeText(getContext(), "Không mở được chi tiết thông báo", Toast.LENGTH_SHORT).show();
            }
        });

        rv.setAdapter(adapter);

        listenNotifications();

        return v;
    }

    private void listenNotifications() {
        if (notifyRef == null) return;

        notifyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotificationItem n = child.getValue(NotificationItem.class);
                    if (n == null) continue;
                    if (n.id == null || n.id.isEmpty()) n.id = child.getKey();
                    list.add(n);
                }

                Collections.sort(list, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        notifyRef.addValueEventListener(notifyListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notifyRef != null && notifyListener != null) {
            notifyRef.removeEventListener(notifyListener);
        }
    }
}
