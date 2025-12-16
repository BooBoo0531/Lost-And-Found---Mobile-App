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

    public void addPost(Post post) {
        List<Post> cur = posts.getValue();
        ArrayList<Post> updated = new ArrayList<>(cur != null ? cur : new ArrayList<>());
        updated.add(0, post); // đưa lên đầu
        posts.setValue(updated);
    }

    public void setPosts(List<Post> newPosts) {
        posts.setValue(new ArrayList<>(newPosts));
    }
}
