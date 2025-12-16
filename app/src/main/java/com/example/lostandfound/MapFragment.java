package com.example.lostandfound;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import vn.vietmap.vietmapsdk.Vietmap;
import vn.vietmap.vietmapsdk.annotations.Marker;
import vn.vietmap.vietmapsdk.annotations.MarkerOptions;
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory;
import vn.vietmap.vietmapsdk.geometry.LatLng;
import vn.vietmap.vietmapsdk.location.LocationComponent;
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions;
import vn.vietmap.vietmapsdk.location.modes.CameraMode;
import vn.vietmap.vietmapsdk.location.modes.RenderMode;
import vn.vietmap.vietmapsdk.maps.MapView;
import vn.vietmap.vietmapsdk.maps.OnMapReadyCallback;
import vn.vietmap.vietmapsdk.maps.Style;
import vn.vietmap.vietmapsdk.maps.VietMapGL;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    // ✅ KHÔNG XUỐNG DÒNG trong apikey
    private static final String STYLE_URL =
            "https://maps.vietmap.vn/api/maps/light/styles.json?apikey=f77a52c999a3400b244172210d4a0ebabb2f0c43926e1d45";
    private MapView mapView;
    private VietMapGL vietMapGL;
    private LocationComponent locationComponent;

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    private TextView tvProvinceTitle;
    private FloatingActionButton btnMyLocation;
    private TextView tvSearch;

    private final LatLng startPoint = new LatLng(10.8018, 106.7143);

    // ===== Shared data từ HomeActivity =====
    private SharedPostViewModel postVM;
    private final List<Post> allPosts = new ArrayList<>();

    // keyword hiện tại khi click marker
    private String currentKeyword = null;

    // nếu chưa có dữ liệu thật thì dùng mock
    private boolean useMock = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vietmap.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // ===== UI =====
        LinearLayout bottomSheetPanel = view.findViewById(R.id.bottomSheetContainer);
        tvProvinceTitle = view.findViewById(R.id.tvProvinceTitle);
        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        tvSearch = view.findViewById(R.id.tvSearchMap);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetPanel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        RecyclerView rvPosts = view.findViewById(R.id.rvPostsLocation);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(getContext(), postList);
        rvPosts.setAdapter(postAdapter);

        // ===== Vietmap MapView =====
        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // ===== Observe SharedPostViewModel (để post mới tự hiện) =====
        postVM = new ViewModelProvider(requireActivity()).get(SharedPostViewModel.class);
        postVM.getPosts().observe(getViewLifecycleOwner(), posts -> {
            allPosts.clear();
            if (posts != null) allPosts.addAll(posts);

            // Có dữ liệu thật -> tắt mock
            useMock = allPosts.isEmpty();
            applyFilterAndRender();
        });

        // ===== Events =====
        btnMyLocation.setOnClickListener(v -> moveToMyLocationOrFallback());
        tvSearch.setOnClickListener(v ->
                Toast.makeText(getContext(), "Mở màn hình tìm kiếm...", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    @Override
    public void onMapReady(@NonNull VietMapGL map) {
        this.vietMapGL = map;

        vietMapGL.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
            enableLocationComponent(style);

            // Camera ban đầu
            vietMapGL.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 15));

            // Markers demo
            addMarker(new LatLng(10.8018, 106.7143), "Ví da nâu", "HUTECH Khu E");
            addMarker(new LatLng(10.8010, 106.7135), "Chìa khóa xe", "Landmark 81");
            addMarker(new LatLng(10.8025, 106.7150), "Mèo anh lông ngắn", "Chung cư City Garden");

            // Mới vào: show tất cả
            tvProvinceTitle.setText("Tin mới quanh đây");
            currentKeyword = null;
            applyFilterAndRender();
        });

        vietMapGL.setOnMarkerClickListener(marker -> {
            currentKeyword = marker.getTitle();
            tvProvinceTitle.setText(marker.getTitle() + " - " + marker.getSnippet());

            vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 17));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);

            applyFilterAndRender();
            return false;
        });
    }

    private void addMarker(LatLng point, String itemTitle, String locationName) {
        if (vietMapGL == null) return;
        vietMapGL.addMarker(
                new MarkerOptions()
                        .position(point)
                        .title(itemTitle)
                        .snippet(locationName)
        );
    }

    private void enableLocationComponent(@NonNull Style style) {
        if (vietMapGL == null) return;

        locationComponent = vietMapGL.getLocationComponent();
        if (locationComponent == null) return;

        if (!hasLocationPermission()) return;

        LocationComponentActivationOptions options =
                LocationComponentActivationOptions.builder(requireContext(), style).build();

        locationComponent.activateLocationComponent(options);

        // ✅ FIX theo cách 2: bật location bằng reflection
        setLocationEnabledCompat(locationComponent, true);

        locationComponent.setCameraMode(CameraMode.TRACKING_GPS_NORTH);
        locationComponent.setRenderMode(RenderMode.GPS);
    }

    // ===== CÁCH 2 (COMPAT) =====
    private void setLocationEnabledCompat(@NonNull Object lc, boolean enabled) {
        try {
            Method m = lc.getClass().getMethod("setLocationComponentEnabled", boolean.class);
            m.invoke(lc, enabled);
            return;
        } catch (Exception ignored) {}

        try {
            Method m = lc.getClass().getDeclaredMethod("setLocationComponentEnabled", boolean.class);
            m.setAccessible(true);
            m.invoke(lc, enabled);
            return;
        } catch (Exception ignored) {}

        try {
            Method m = lc.getClass().getMethod("setIsLocationComponentEnabled", boolean.class);
            m.invoke(lc, enabled);
            return;
        } catch (Exception ignored) {}

        try {
            Field f = lc.getClass().getField("isLocationComponentEnabled");
            f.setBoolean(lc, enabled);
            return;
        } catch (Exception ignored) {}

        try {
            Field f = lc.getClass().getDeclaredField("isLocationComponentEnabled");
            f.setAccessible(true);
            f.setBoolean(lc, enabled);
        } catch (Exception ignored) {}
    }

    private void moveToMyLocationOrFallback() {
        if (vietMapGL == null) return;

        if (!hasLocationPermission()) {
            Toast.makeText(getContext(), "Chưa có quyền vị trí (Location permission).", Toast.LENGTH_SHORT).show();
            vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 17));
            return;
        }

        if (locationComponent != null) {
            Location last = locationComponent.getLastKnownLocation();
            if (last != null) {
                LatLng myLatLng = new LatLng(last.getLatitude(), last.getLongitude());
                vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 17));
                Toast.makeText(getContext(), "Đang lấy vị trí của bạn...", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 17));
        Toast.makeText(getContext(), "Không lấy được GPS, quay về điểm mặc định.", Toast.LENGTH_SHORT).show();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // ===== Render list theo keyword + nguồn dữ liệu (real/mock) =====
    private void applyFilterAndRender() {
        List<Post> source = useMock ? createMockPosts(currentKeyword) : allPosts;

        List<Post> result = new ArrayList<>();
        if (currentKeyword == null || currentKeyword.trim().isEmpty()) {
            result.addAll(source);
        } else {
            String k = currentKeyword.toLowerCase();

            for (Post p : source) {
                String content = safeLower(p.getContent());
                String address = safeLower(p.getAddress());
                if (content.contains(k) || address.contains(k)) {
                    result.add(p);
                }
            }

            // nếu lọc không ra gì thì show tất cả
            if (result.isEmpty()) result.addAll(source);
        }

        postList.clear();
        postList.addAll(result);
        postAdapter.notifyDataSetChanged();
    }

    // ===== Mock đúng constructor 8 tham số của Post =====
    private List<Post> createMockPosts(@Nullable String keyword) {
        List<Post> dummy = new ArrayList<>();

        if (keyword != null && keyword.contains("Ví")) {
            dummy.add(new Post("id1", "Nguyễn Văn A", "10 phút trước",
                    "Mình đánh rơi ví da màu nâu tại sảnh E...", "LOST",
                    null, "090123456", "HUTECH Khu E"));
            return dummy;
        }

        if (keyword != null && keyword.contains("Chìa")) {
            dummy.add(new Post("id2", "Trần Thị B", "2 giờ trước",
                    "Nhặt được chìa khoá xe Honda Vision...", "FOUND",
                    null, "0909888777", "Landmark 81"));
            return dummy;
        }

        if (keyword != null && keyword.contains("Mèo")) {
            dummy.add(new Post("id3", "Lê C", "1 ngày trước",
                    "Tìm mèo lạc, có hậu tạ...", "LOST",
                    null, "0912345678", "Chung cư City Garden"));
            return dummy;
        }

        // Default
        dummy.add(new Post("id4", "User 1", "Vừa xong", "Rơi tai nghe AirPods...", "LOST", null, "0123", "Gần đây"));
        dummy.add(new Post("id5", "User 2", "15p trước", "Nhặt được thẻ gửi xe...", "FOUND", null, "0456", "Gần đây"));
        dummy.add(new Post("id6", "User 3", "1h trước", "Tìm chó lạc...", "LOST", null, "0789", "Gần đây"));
        return dummy;
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    // ===== MapView lifecycle =====
    @Override public void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override public void onStop() { super.onStop(); if (mapView != null) mapView.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
    }
}
