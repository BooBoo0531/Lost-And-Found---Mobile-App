package com.example.lostandfound;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Thư viện OSMDROID
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private MapView map;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private PostAdapter postAdapter;
    // 1. THÊM BIẾN NÀY ĐỂ QUẢN LÝ DỮ LIỆU
    private List<Post> postList;

    private TextView tvProvinceTitle;
    private FloatingActionButton btnMyLocation;
    private TextView tvSearch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        LinearLayout bottomSheetPanel = view.findViewById(R.id.bottomSheetContainer);
        tvProvinceTitle = view.findViewById(R.id.tvProvinceTitle);
        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        tvSearch = view.findViewById(R.id.tvSearchMap);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetPanel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // --- CẤU HÌNH RECYCLERVIEW (ĐÃ SỬA) ---
        RecyclerView rvPosts = view.findViewById(R.id.rvPostsLocation);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. KHỞI TẠO LIST VÀ ADAPTER ĐÚNG CÁCH (Fix lỗi biên dịch)
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(getContext(), postList);
        rvPosts.setAdapter(postAdapter);

        // --- CẤU HÌNH BẢN ĐỒ ---
        map = view.findViewById(R.id.map);

        XYTileSource cartoDbLight = new XYTileSource(
                "CartoDbLight",
                1, 20, 256, ".png",
                new String[] {
                        "https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/",
                        "https://cartodb-basemaps-b.global.ssl.fastly.net/light_all/",
                        "https://cartodb-basemaps-c.global.ssl.fastly.net/light_all/"
                },
                "© OpenStreetMap contributors, © CARTO"
        );
        map.setTileSource(cartoDbLight);

        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);
        map.getController().setZoom(15.0);

        GeoPoint startPoint = new GeoPoint(10.8018, 106.7143);
        map.getController().setCenter(startPoint);

        // --- SỰ KIỆN ---
        btnMyLocation.setOnClickListener(v -> {
            map.getController().animateTo(startPoint);
            map.getController().setZoom(17.0);
            Toast.makeText(getContext(), "Đang lấy vị trí của bạn...", Toast.LENGTH_SHORT).show();
        });

        tvSearch.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Mở màn hình tìm kiếm...", Toast.LENGTH_SHORT).show();
        });

        // --- THÊM MARKER ---
        addMarker(new GeoPoint(10.8018, 106.7143), "Ví da nâu", "HUTECH Khu E");
        addMarker(new GeoPoint(10.8010, 106.7135), "Chìa khóa xe", "Landmark 81");
        addMarker(new GeoPoint(10.8025, 106.7150), "Mèo anh lông ngắn", "Chung cư City Garden");

        loadMockData("Gần bạn");

        return view;
    }

    private void addMarker(GeoPoint point, String itemTitle, String locationName) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(itemTitle);
        marker.setSnippet(locationName);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setOnMarkerClickListener((m, mapView) -> {
            tvProvinceTitle.setText(m.getTitle() + " - " + m.getSnippet());
            loadMockData(m.getTitle());
            map.getController().animateTo(m.getPosition());
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            m.showInfoWindow();
            return true;
        });
        map.getOverlays().add(marker);
    }

    private void loadMockData(String keyword) {
        List<Post> dummyPosts = new ArrayList<>();

        if (keyword.contains("Ví")) {
            // Insert empty userEmail "" to match Post constructor signature (9 params)
            dummyPosts.add(new Post("id1", "Nguyễn Văn A", "", "10 phút trước", "Mình đánh rơi ví da màu nâu tại sảnh E...", "LOST", null, "090123456", "HUTECH Khu E"));
        } else if (keyword.contains("Chìa")) {
            dummyPosts.add(new Post("id2", "Trần Thị B", "", "2 giờ trước", "Nhặt được chìa khoá xe Honda Vision...", "FOUND", null, "0909888777", "Landmark 81"));
        } else if (keyword.contains("Mèo")) {
            dummyPosts.add(new Post("id3", "Lê C", "", "1 ngày trước", "Tìm mèo lạc, có hậu tạ...", "LOST", null, "0912345678", "Chung cư City Garden"));
        } else {
            dummyPosts.add(new Post("id4", "User 1", "", "Vừa xong", "Rơi tai nghe AirPods...", "LOST", null, "0123", "Gần đây"));
            dummyPosts.add(new Post("id5", "User 2", "", "15p trước", "Nhặt được thẻ gửi xe...", "FOUND", null, "0456", "Gần đây"));
            dummyPosts.add(new Post("id6", "User 3", "", "1h trước", "Tìm chó lạc...", "LOST", null, "0789", "Gần đây"));
        }

        // 3. CẬP NHẬT DỮ LIỆU ĐÚNG CÁCH (Thay vì gọi setPostList)
        postList.clear();              // Xóa dữ liệu cũ
        postList.addAll(dummyPosts);   // Thêm dữ liệu mới
        postAdapter.notifyDataSetChanged(); // Báo adapter vẽ lại
    }

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
