package com.example.lostandfound;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SharedPostViewModel extends ViewModel {

    private final MutableLiveData<List<Post>> posts = new MutableLiveData<>(new ArrayList<>());

    private final List<Post> originalList = new ArrayList<>();

    private boolean isSearching = false;

    public LiveData<List<Post>> getPosts() {
        return posts;
    }

    public boolean isSearching() {
        return isSearching;
    }

    public void setPosts(List<Post> list) {
        originalList.clear();
        if (list != null) {
            originalList.addAll(list);
        }
        isSearching = false;
        posts.setValue(new ArrayList<>(originalList));
    }

    public void addPost(Post p) {
        originalList.add(0, p);
        if (!isSearching) {
            posts.setValue(new ArrayList<>(originalList));
        }
    }

    public void search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            isSearching = false;
            posts.setValue(new ArrayList<>(originalList));
            return;
        }

        isSearching = true;
        String key = removeAccent(keyword.toLowerCase());
        List<Post> filtered = new ArrayList<>();

        for (Post p : originalList) {
            String desc = removeAccent(safe(p.getDescription()).toLowerCase());
            String addr = removeAccent(safe(p.getAddress()).toLowerCase());
            String email = safe(p.getUserEmail()).toLowerCase();

            if (desc.contains(key) || addr.contains(key) || email.contains(key)) {
                filtered.add(p);
            }
        }
        posts.setValue(filtered);
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ','d').replace('Đ','D');
    }
}