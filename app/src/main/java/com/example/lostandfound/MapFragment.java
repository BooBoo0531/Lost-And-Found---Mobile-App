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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final String STYLE_URL =
            "https://maps.vietmap.vn/api/maps/light/styles.json?apikey=f77a52c999a3400b244172210d4a0ebabb2f0c43926e1d45";

    private static final float RADIUS_METERS = 5000f; // 5km
    private static final int REQ_LOCATION = 1101;

    private MapView mapView;
    private VietMapGL vietMapGL;
    private LocationComponent locationComponent;

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();
    private final List<Post> allPosts = new ArrayList<>();

    private TextView tvProvinceTitle;
    private TextView tvEmptyNearby;
    private FloatingActionButton btnMyLocation;
    private TextView tvSearch;

    private FusedLocationProviderClient fusedClient;
    private LatLng myLatLng = null;

    private final List<Marker> markers = new ArrayList<>();
    private final Map<Long, String> markerIdToPostId = new HashMap<>();
    private String focusedPostId = null;

    private SharedPostViewModel postVM;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Vietmap.getInstance(requireContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // BottomSheet
        LinearLayout bottomSheetPanel = view.findViewById(R.id.bottomSheetContainer);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetPanel);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Views
        tvProvinceTitle = view.findViewById(R.id.tvProvinceTitle);
        tvEmptyNearby = view.findViewById(R.id.tvEmptyNearby);
        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        tvSearch = view.findViewById(R.id.tvSearchMap);

        // Recycler
        rvPosts = view.findViewById(R.id.rvPostsLocation);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(requireContext(), postList);
        rvPosts.setAdapter(postAdapter);

        // MapView
        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // ViewModel: lấy danh sách bài
        postVM = new ViewModelProvider(requireActivity()).get(SharedPostViewModel.class);
        postVM.getPosts().observe(getViewLifecycleOwner(), posts -> {
            allPosts.clear();
            if (posts != null) allPosts.addAll(posts);
            applyFilterAndRender();
        });

        // Buttons
        btnMyLocation.setOnClickListener(v -> moveToMyLocationAndFilter());
        tvSearch.setOnClickListener(v ->
                Toast.makeText(getContext(), "Mở màn hình tìm kiếm...", Toast.LENGTH_SHORT).show()
        );

        // Khi người dùng kéo sheet về collapsed -> bỏ focus marker (show lại list theo 5km)
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (focusedPostId != null) {
                        focusedPostId = null;
                        applyFilterAndRender();
                    }
                }
            }
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull VietMapGL map) {
        this.vietMapGL = map;

        vietMapGL.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
            enableLocationComponent(style);

            updateTitle();
            focusedPostId = null;

            // Lấy vị trí -> lọc 5km -> render
            requestMyLocationAndRefresh();
        });

        vietMapGL.setOnMarkerClickListener(marker -> {
            String postId = markerIdToPostId.get(marker.getId());
            if (postId != null) {
                focusedPostId = postId;
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                applyFilterAndRender();
            }
            return false;
        });

        // Bấm lên map để bỏ focus
        try {
            vietMapGL.addOnMapClickListener(point -> {
                if (focusedPostId != null) {
                    focusedPostId = null;
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    applyFilterAndRender();
                }
                return false;
            });
        } catch (Exception ignored) {}
    }

    private void updateTitle() {
        if (tvProvinceTitle == null) return;

        if (myLatLng != null) {
            tvProvinceTitle.setText("Tin mới quanh đây (≤ 5km)");
        } else {
            tvProvinceTitle.setText("Tin mới quanh đây");
        }
    }

    private void requestMyLocationAndRefresh() {
        if (!hasLocationPermission()) {
            // Xin quyền để lọc 5km
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOCATION);

            // Chưa có quyền -> tạm show tất cả (không lọc)
            myLatLng = null;
            updateTitle();
            applyFilterAndRender();
            return;
        }

        fusedClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        myLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());

                        if (vietMapGL != null) {
                            vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
                        }
                    } else {
                        // Không lấy được vị trí -> không lọc 5km
                        myLatLng = null;
                    }
                    updateTitle();
                    applyFilterAndRender();
                })
                .addOnFailureListener(e -> {
                    myLatLng = null;
                    updateTitle();
                    applyFilterAndRender();
                });
    }

    private void moveToMyLocationAndFilter() {
        if (!hasLocationPermission()) {
            Toast.makeText(getContext(), "Chưa có quyền vị trí", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOCATION);
            return;
        }
        requestMyLocationAndRefresh();
    }

    private void applyFilterAndRender() {
        List<Post> source = new ArrayList<>(allPosts);

        // 1) focus theo marker: chỉ 1 bài
        if (focusedPostId != null) {
            List<Post> only = new ArrayList<>();
            for (Post p : source) {
                if (p != null && focusedPostId.equals(p.getId())) {
                    only.add(p);
                    break;
                }
            }

            postList.clear();
            postList.addAll(only);
            postAdapter.notifyDataSetChanged();
            renderMarkers(only);
            updateEmptyState(only.isEmpty());
            return;
        }

        // 2) nếu có vị trí -> lọc trong 5km
        if (myLatLng != null) {
            List<Post> nearby = new ArrayList<>();
            for (Post p : source) {
                if (!isValidLatLng(p)) continue;

                float d = distanceMeters(
                        myLatLng.getLatitude(), myLatLng.getLongitude(),
                        p.getLat(), p.getLng()
                );
                if (d <= RADIUS_METERS) nearby.add(p);
            }

            postList.clear();
            postList.addAll(nearby);
            postAdapter.notifyDataSetChanged();
            renderMarkers(nearby);
            updateEmptyState(nearby.isEmpty());
            return;
        }

        // 3) chưa có vị trí -> show tất cả (không lọc)
        postList.clear();
        postList.addAll(source);
        postAdapter.notifyDataSetChanged();
        renderMarkers(source);
        updateEmptyState(source.isEmpty());
    }

    private void updateEmptyState(boolean empty) {
        if (tvEmptyNearby == null) return;
        tvEmptyNearby.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderMarkers(List<Post> posts) {
        if (vietMapGL == null) return;

        // clear old markers
        for (Marker m : markers) {
            try { vietMapGL.removeMarker(m); } catch (Exception ignored) {}
        }
        markers.clear();
        markerIdToPostId.clear();

        // add new markers
        for (Post p : posts) {
            if (!isValidLatLng(p)) continue;

            String title = safe(p.getPostType()) + " - " + shortText(p.getDescription());
            String snippet = safe(p.getAddress());

            Marker m = vietMapGL.addMarker(new MarkerOptions()
                    .position(new LatLng(p.getLat(), p.getLng()))
                    .title(title)
                    .snippet(snippet));

            if (m != null) {
                markers.add(m);
                markerIdToPostId.put(m.getId(), p.getId());
            }
        }
    }

    private String shortText(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= 20) return s;
        return s.substring(0, 20) + "...";
    }

    private boolean isValidLatLng(Post p) {
        return p != null
                && !(p.getLat() == 0 && p.getLng() == 0)
                && p.getLat() >= -90 && p.getLat() <= 90
                && p.getLng() >= -180 && p.getLng() <= 180;
    }

    private float distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        float[] res = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, res);
        return res[0];
    }

    private void enableLocationComponent(@NonNull Style style) {
        if (vietMapGL == null) return;

        locationComponent = vietMapGL.getLocationComponent();
        if (locationComponent == null) return;

        if (!hasLocationPermission()) return;

        LocationComponentActivationOptions options =
                LocationComponentActivationOptions.builder(requireContext(), style).build();

        locationComponent.activateLocationComponent(options);
        setLocationEnabledCompat(locationComponent, true);

        locationComponent.setCameraMode(CameraMode.TRACKING_GPS_NORTH);
        locationComponent.setRenderMode(RenderMode.GPS);
    }

    // Vietmap SDK có thể khác version -> bật location component theo nhiều cách
    private void setLocationEnabledCompat(@NonNull Object lc, boolean enabled) {
        try { Method m = lc.getClass().getMethod("setLocationComponentEnabled", boolean.class); m.invoke(lc, enabled); return; }
        catch (Exception ignored) {}

        try { Method m = lc.getClass().getDeclaredMethod("setLocationComponentEnabled", boolean.class); m.setAccessible(true); m.invoke(lc, enabled); return; }
        catch (Exception ignored) {}

        try { Method m = lc.getClass().getMethod("setIsLocationComponentEnabled", boolean.class); m.invoke(lc, enabled); return; }
        catch (Exception ignored) {}

        try { Field f = lc.getClass().getField("isLocationComponentEnabled"); f.setBoolean(lc, enabled); return; }
        catch (Exception ignored) {}

        try { Field f = lc.getClass().getDeclaredField("isLocationComponentEnabled"); f.setAccessible(true); f.setBoolean(lc, enabled); }
        catch (Exception ignored) {}
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private String safe(String s) { return s == null ? "" : s; }

    // Permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            boolean granted = false;
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                // bật location + refresh lọc 5km
                if (vietMapGL != null) {
                    Style style = vietMapGL.getStyle();
                    if (style != null) enableLocationComponent(style);
                }
                requestMyLocationAndRefresh();
            } else {
                Toast.makeText(getContext(), "Bạn chưa cấp quyền vị trí, không lọc 5km được.", Toast.LENGTH_SHORT).show();
                myLatLng = null;
                updateTitle();
                applyFilterAndRender();
            }
        }
    }

    // MapView lifecycle
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

        // clear markers
        if (vietMapGL != null) {
            for (Marker m : markers) {
                try { vietMapGL.removeMarker(m); } catch (Exception ignored) {}
            }
        }
        markers.clear();
        markerIdToPostId.clear();

        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        vietMapGL = null;
    }
}
