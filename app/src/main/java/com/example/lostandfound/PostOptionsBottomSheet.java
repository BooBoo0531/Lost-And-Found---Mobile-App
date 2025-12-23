package com.example.lostandfound;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PostOptionsBottomSheet extends BottomSheetDialogFragment {

    private Post post;
    private OnOptionClickListener listener;

    public interface OnOptionClickListener {
        void onEdit(Post post);
        void onDelete(Post post);
    }

    public static PostOptionsBottomSheet newInstance(Post post) {
        PostOptionsBottomSheet fragment = new PostOptionsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("POST_DATA", post);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnOptionClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            post = (Post) getArguments().getSerializable("POST_DATA");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_bottom_sheet_post_options, container, false);

        v.findViewById(R.id.btnOptionEdit).setOnClickListener(view -> {
            if (listener != null) listener.onEdit(post);
            dismiss();
        });

        v.findViewById(R.id.btnOptionDelete).setOnClickListener(view -> {
            if (listener != null) listener.onDelete(post);
            dismiss();
        });

        return v;
    }
}