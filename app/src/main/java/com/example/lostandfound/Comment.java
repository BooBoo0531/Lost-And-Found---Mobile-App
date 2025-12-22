package com.example.lostandfound;

public class Comment {
    public String id;
    public String postId;
    public String userId;
    public String userEmail;
    public String content;

    // ✅ Hỗ trợ dữ liệu cũ (nếu DB từng lưu key "text" thay vì "content")
    public String text;

    public String imageBase64;
    public String parentId;   // null/"" = comment, có giá trị = reply
    public long timestamp;

    public Comment() {}

    public Comment(String id, String postId, String userId, String userEmail,
                   String content, String imageBase64, String parentId, long timestamp) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.content = content;
        this.imageBase64 = imageBase64;
        this.parentId = parentId;
        this.timestamp = timestamp;
    }
}
