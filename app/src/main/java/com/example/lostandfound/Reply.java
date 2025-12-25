package com.example.lostandfound;

import java.io.Serializable;

public class Reply implements Serializable {
    public String id;
    public String commentId;      // ID của comment cha (optional)
    public String userId;
    public String userEmail;
    public String text;
    public String imageBase64;
    public long createdAt;

    // ✅ THÊM 2 field này để reply-to-reply biết đang trả lời ai
    public String replyToUserId;
    public String replyToEmail;

    // Constructor rỗng bắt buộc cho Firebase
    public Reply() {}

    public Reply(String id,
                 String commentId,
                 String userId,
                 String userEmail,
                 String text,
                 String imageBase64,
                 long createdAt,
                 String replyToUserId,
                 String replyToEmail) {
        this.id = id;
        this.commentId = commentId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.text = text;
        this.imageBase64 = imageBase64;
        this.createdAt = createdAt;
        this.replyToUserId = replyToUserId;
        this.replyToEmail = replyToEmail;
    }
}
