package com.example.lostandfound;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

// Thư viện OSMDROID
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private MapView map;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private PostAdapter postAdapter;
    private TextView tvProvinceTitle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Cấu hình User Agent để tránh bị server OSM chặn (Quan trọng)
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Kết nối với layout fragment_map.xml
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // 1. Cấu hình BottomSheet (Bảng thông tin trượt)
        LinearLayout bottomSheetPanel = view.findViewById(R.id.bottomSheetContainer);
        tvProvinceTitle = view.findViewById(R.id.tvProvinceTitle);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetPanel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Ẩn mặc định

        // 2. Cấu hình RecyclerView (Danh sách bài đăng)
        RecyclerView rvPosts = view.findViewById(R.id.rvPostsLocation);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter();
        rvPosts.setAdapter(postAdapter);

        // 3. Cấu hình Bản đồ (OSM Map)
        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK); // Chọn loại bản đồ
        map.setMultiTouchControls(true); // Cho phép zoom bằng 2 ngón tay

        // Đặt vị trí camera ban đầu (Zoom vào giữa Việt Nam)
        map.getController().setZoom(6.0);
        GeoPoint startPoint = new GeoPoint(16.0471, 108.2068); // Đà Nẵng làm tâm
        map.getController().setCenter(startPoint);

        // --- THÊM MARKER CÁC TỈNH ---
        addMarker(new GeoPoint(21.0285, 105.8542), "Hà Nội");
        addMarker(new GeoPoint(10.7769, 106.7009), "TP. Hồ Chí Minh");
        addMarker(new GeoPoint(16.0544, 108.2022), "Đà Nẵng");
        addMarker(new GeoPoint(10.0452, 105.7469), "Cần Thơ");

        return view;
    }

    // Hàm hỗ trợ thêm Marker và xử lý sự kiện Click
    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Khi bấm vào Marker
        marker.setOnMarkerClickListener((m, mapView) -> {
            // 1. Cập nhật tiêu đề bảng
            tvProvinceTitle.setText("Bài đăng tại: " + m.getTitle());

            // 2. Load dữ liệu bài đăng giả lập (Sau này thay bằng API)
            loadMockData(m.getTitle());

            // 3. Trượt bảng thông tin lên (Chế độ hiển thị 1 nửa)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);

            return true;
        });
        map.getOverlays().add(marker);
    }

    // Hàm tạo dữ liệu giả để test
    private void loadMockData(String location) {
        List<Post> dummyPosts = new ArrayList<>();

        // Tạo bài đăng khác nhau tùy địa điểm
        if (location.equals("TP. Hồ Chí Minh")) {
            dummyPosts.add(new Post("Nguyễn Văn A", "10 phút trước", "Rơi ví màu đen tại Quận 1, ai thấy liên hệ...", "LOST"));
            dummyPosts.add(new Post("Trần Thị B", "2 giờ trước", "Nhặt được thẻ sinh viên HUTECH", "FOUND"));
            dummyPosts.add(new Post("Lê C", "5 giờ trước", "Tìm mèo lạc màu vàng...", "LOST"));
        } else if (location.equals("Hà Nội")) {
            dummyPosts.add(new Post("Phạm Văn D", "1 ngày trước", "Rơi giấy tờ xe tại Cầu Giấy", "LOST"));
            dummyPosts.add(new Post("Hoàng E", "3 ngày trước", "Nhặt được chìa khoá xe Honda", "FOUND"));
        } else {
            dummyPosts.add(new Post("User ẩn danh", "Vừa xong", "Có ai thấy chó Bull Pháp lạc không ạ?", "LOST"));
        }

        postAdapter.setPostList(dummyPosts);
    }

    // Quản lý vòng đời map (để không bị lỗi bộ nhớ)
    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}