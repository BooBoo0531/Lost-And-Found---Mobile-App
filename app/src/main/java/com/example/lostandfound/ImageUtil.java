package com.example.lostandfound;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

public class ImageUtil {

    // Chuyển Bitmap -> String (Dùng khi Lưu)
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Nén JPEG 50%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        // Dùng NO_WRAP để chuỗi sạch, không xuống dòng
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    // Chuyển String -> Bitmap (Dùng khi Hiển thị)
    public static Bitmap base64ToBitmap(String base64Str) {
        try {
            if (base64Str == null || base64Str.isEmpty()) return null;
            // Dùng NO_WRAP để khớp với lúc nén
            byte[] decodedBytes = Base64.decode(base64Str, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}