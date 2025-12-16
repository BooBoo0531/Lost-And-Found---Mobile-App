package com.example.lostandfound;

import java.io.Serializable;

public class Post implements Serializable {

    // Khuyến nghị thêm serialVersionUID để ổn định khi build
    private static final long serialVersionUID = 1L;

    private String id;          // ID riêng của bài post (Firebase tạo)
    private String userName;
    private String timePosted;
    private String content;
    private String status;      // "LOST" hoặc "FOUND"
    private String imageBase64; // Ảnh dạng base64
    private String contactPhone;// SĐT liên hệ
    private String address;     // Địa điểm

    // 1) BẮT BUỘC: Constructor rỗng cho Firebase
    public Post() {}

    // 2) Constructor đầy đủ
    public Post(String id,
                String userName,
                String timePosted,
                String content,
                String status,
                String imageBase64,
                String contactPhone,
                String address) {
        this.id = id;
        this.userName = userName;
        this.timePosted = timePosted;
        this.content = content;
        this.status = status;
        this.imageBase64 = imageBase64;
        this.contactPhone = contactPhone;
        this.address = address;
    }

    // 3) Getter / Setter
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
