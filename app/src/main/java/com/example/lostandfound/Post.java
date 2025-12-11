package com.example.lostandfound;

public class Post {
    private String id;          // ID riêng của bài post (do Firebase tạo)
    private String userName;
    private String timePosted;
    private String content;
    private String status;      // "LOST" hoặc "FOUND"
    private String imageBase64; // Lưu ảnh dưới dạng chuỗi ký tự dài
    private String contactPhone;// Số điện thoại liên hệ
    private String address;     // Địa điểm bị mất/nhặt được

    // 1. BẮT BUỘC: Constructor rỗng cho Firebase
    public Post() {
    }

    // 2. Constructor đầy đủ để mình dùng lúc đăng bài
    public Post(String id, String userName, String timePosted, String content, String status, String imageBase64, String contactPhone, String address) {
        this.id = id;
        this.userName = userName;
        this.timePosted = timePosted;
        this.content = content;
        this.status = status;
        this.imageBase64 = imageBase64;
        this.contactPhone = contactPhone;
        this.address = address;
    }

    // 3. Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getTimePosted() { return timePosted; }
    public void setTimePosted(String timePosted) { this.timePosted = timePosted; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}