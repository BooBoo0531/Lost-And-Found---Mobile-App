package com.example.lostandfound;

public class Post {
    private String userName;
    private String timePosted;
    private String content;
    private String status; // Giá trị là "LOST" hoặc "FOUND"

    public Post(String userName, String timePosted, String content, String status) {
        this.userName = userName;
        this.timePosted = timePosted;
        this.content = content;
        this.status = status;
    }

    // Các hàm lấy dữ liệu (Getter)
    public String getUserName() { return userName; }
    public String getTimePosted() { return timePosted; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
}