package com.example.lostandfound;

public class Comment {
    public String id;
    public String postId;
    public String userId;
    public String userEmail;
    public String content;
    public String text;
    public String imageBase64;
    public String parentId;
    public long timestamp;

    // (không bắt buộc dùng trong version nested replies)
    public boolean isViewMore;
    public int replyCount;

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

    public static Comment viewMoreRow(String parentId, int replyCount) {
        Comment c = new Comment();
        c.isViewMore = true;
        c.parentId = parentId;
        c.replyCount = replyCount;
        return c;
    }
}
