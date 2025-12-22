package com.example.lostandfound;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class SharedPostViewModel extends ViewModel {

    private final MutableLiveData<List<Post>> posts = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Post>> getPosts() {
        return posts;
    }

    public void addPost(Post p) {
        List<Post> current = posts.getValue();
        ArrayList<Post> newList = new ArrayList<>();
        if (current != null) newList.addAll(current);

        newList.add(0, p);            // bài mới lên đầu
        posts.setValue(newList);      // ✅ PHẢI setValue thì observer mới chạy
    }

    public void setPosts(List<Post> list) {
        posts.setValue(list == null ? new ArrayList<>() : new ArrayList<>(list));
    }
}
