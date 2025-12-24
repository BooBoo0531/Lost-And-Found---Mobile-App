package com.example.lostandfound;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

public class MapFragment extends Fragment
        implements OnMapReadyCallback, VietMapGL.OnMapClickListener {

    // Tile key
    private static final String STYLE_URL =
            "https://maps.vietmap.vn/api/maps/light/styles.json?apikey=f77a52c999a3400b244172210d4a0ebabb2f0c43926e1d45";
    // Service key
    private static final String SERVICES_KEY =
            "ba1cf0075ef140e2bccc1b2a4392454a11e042a82fb7674a";

    private static final float RADIUS_METERS = 5000f;
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

    // Search View
    private EditText edtSearchMap;
    private ImageView btnClearSearch;
    private ImageView btnCameraSearchMap; // Thêm nút Camera

    private FusedLocationProviderClient fusedClient;
    private LatLng myLatLng = null;
    private final List<Marker> markers = new ArrayList<>();
    private final Map<Long, String> markerIdToPostId = new HashMap<>();
    private String focusedPostId = null;
    private String selectedWard = null;
    private boolean forceNearbyTitle = false;
    private SharedPostViewModel postVM;
    private final OkHttpClient httpClient = new OkHttpClient();
    private Call pendingReverseCall = null;
    private ImageView btnCameraSearch;

    // Image Launcher
    private ActivityResultLauncher<String> searchImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vietmap.getInstance(requireContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Đăng ký nhận ảnh từ thư viện
        searchImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        analyzeImageForSearch(uri);
                    }
                }
        );
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

        edtSearchMap = view.findViewById(R.id.edtSearchMap);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnCameraSearchMap = view.findViewById(R.id.btnCameraSearch); // Ánh xạ nút Camera

        // Recycler
        rvPosts = view.findViewById(R.id.rvPostsLocation);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(requireContext(), postList);
        rvPosts.setAdapter(postAdapter);

        // MapView
        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        postVM = new ViewModelProvider(requireActivity()).get(SharedPostViewModel.class);
        postVM.getPosts().observe(getViewLifecycleOwner(), posts -> {
            allPosts.clear();
            if (posts != null) allPosts.addAll(posts);
            applyFilterAndRender();
        });

        setupSearchLogic();

        btnMyLocation.setOnClickListener(v -> {
            focusedPostId = null;
            selectedWard = null;
            forceNearbyTitle = true;
            updateTitle();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            moveToMyLocationAndFilter();
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (focusedPostId != null || selectedWard != null) {
                        focusedPostId = null;
                        selectedWard = null;
                        updateTitle();
                        applyFilterAndRender();
                    }
                }
            }
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        return view;
    }

    private void setupSearchLogic() {
        if (edtSearchMap != null) {
            edtSearchMap.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String keyword = s.toString();
                    if (postVM != null) postVM.search(keyword);
                    if (btnClearSearch != null) btnClearSearch.setVisibility(keyword.isEmpty() ? View.GONE : View.VISIBLE);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (edtSearchMap != null) edtSearchMap.setText("");
                if (postVM != null) postVM.search("");
            });
        }

        // Sự kiện nút Camera
        if (btnCameraSearchMap != null) {
            btnCameraSearchMap.setOnClickListener(v -> {
                searchImageLauncher.launch("image/*");
            });
        }
    }

    // --- LOGIC NHẬN DIỆN ẢNH (GIỐNG BÊN HOME) ---
    private void analyzeImageForSearch(android.net.Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            labeler.process(image)
                    .addOnSuccessListener(labels -> {
                        if (!labels.isEmpty()) {
                            String englishKeyword = labels.get(0).getText();
                            manualTranslateAndSearch(englishKeyword);
                        } else {
                            Toast.makeText(getContext(), "Không nhận diện được vật thể", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi phân tích ảnh", Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manualTranslateAndSearch(String englishKeyword) {
        String vietnameseKeyword = englishKeyword;
        switch (englishKeyword.toLowerCase()) {
            case "wallet": vietnameseKeyword = "ví"; break;
            case "purse": vietnameseKeyword = "túi xách"; break;
            case "cell phone":
            case "mobile phone": vietnameseKeyword = "điện thoại"; break;
            case "computer":
            case "laptop": vietnameseKeyword = "laptop"; break;
            case "key": vietnameseKeyword = "chìa khóa"; break;
            case "cat": vietnameseKeyword = "mèo"; break;
            case "dog": vietnameseKeyword = "chó"; break;
            case "backpack": vietnameseKeyword = "balo"; break;
            // Thêm các từ khác nếu cần
        }

        if (edtSearchMap != null) {
            edtSearchMap.setText(vietnameseKeyword);
            Toast.makeText(getContext(), "Đã tìm: " + vietnameseKeyword, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull VietMapGL map) {
        this.vietMapGL = map;
        vietMapGL.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
            enableLocationComponent(style);
            focusedPostId = null;
            updateTitle();
            vietMapGL.addOnMapClickListener(MapFragment.this);
            requestMyLocationAndRefresh();
        });
        vietMapGL.setOnMarkerClickListener(marker -> {
            String postId = markerIdToPostId.get(marker.getId());
            if (postId != null) {
                selectedWard = null;
                focusedPostId = postId;
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                applyFilterAndRender();
            }
            return false;
        });
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        forceNearbyTitle = false;
        focusedPostId = null;
        tvProvinceTitle.setText("Đang lấy phường...");
        tvEmptyNearby.setVisibility(View.GONE);
        reverseWard(point);
        return true;
    }

    private void reverseWard(@NonNull LatLng point) {
        if (pendingReverseCall != null) pendingReverseCall.cancel();
        String url = "https://maps.vietmap.vn/api/reverse/v3?apikey=" + SERVICES_KEY + "&lng=" + point.getLongitude() + "&lat=" + point.getLatitude();
        Request request = new Request.Builder().url(url).build();
        pendingReverseCall = httpClient.newCall(request);
        pendingReverseCall.enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!isAdded() || call.isCanceled()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Reverse thất bại", Toast.LENGTH_SHORT).show();
                    selectedWard = null;
                    updateTitle();
                    applyFilterAndRender();
                });
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!isAdded() || call.isCanceled()) return;
                String body = response.body() != null ? response.body().string() : "";
                String display = null;
                try {
                    JSONArray arr = new JSONArray(body);
                    if (arr.length() > 0) {
                        JSONObject obj = arr.getJSONObject(0);
                        display = obj.optString("display", null);
                    }
                } catch (Exception ignored) {}
                final String ward = extractWard(display);
                requireActivity().runOnUiThread(() -> {
                    if (ward == null || ward.trim().isEmpty()) {
                        Toast.makeText(getContext(), "Không lấy được phường", Toast.LENGTH_SHORT).show();
                        selectedWard = null;
                    } else { selectedWard = ward.trim(); }
                    updateTitle();
                    applyFilterAndRender();
                });
            }
        });
    }

    @Nullable
    private String extractWard(@Nullable String text) {
        if (text == null) return null;
        Pattern p = Pattern.compile("(Phường|Xã|Thi\\s*trấn|Thị\\s*trấn)\\s+([^,]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(text);
        if (m.find()) return (m.group(1) + " " + m.group(2)).trim();
        Pattern p2 = Pattern.compile("(?i)\\bP\\.?\\s*(\\d+)\\b");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) return ("Phường " + m2.group(1)).trim();
        return null;
    }

    private String norm(@Nullable String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.trim().toLowerCase(Locale.ROOT);
    }

    private void updateTitle() {
        if (tvProvinceTitle == null) return;
        if (selectedWard != null && !selectedWard.isEmpty()) { tvProvinceTitle.setText("Bài viết tại " + selectedWard); return; }
        if (postVM != null && postVM.isSearching()) { tvProvinceTitle.setText("Kết quả tìm kiếm"); return; }
        if (forceNearbyTitle || myLatLng != null) { tvProvinceTitle.setText("Tin mới quanh đây (≤ 5km)"); return; }
        tvProvinceTitle.setText("Tin mới quanh đây");
    }

    private void applyFilterAndRender() {
        List<Post> source = new ArrayList<>(allPosts);
        if (focusedPostId != null) {
            List<Post> only = new ArrayList<>();
            for (Post p : source) { if (p != null && focusedPostId.equals(p.getId())) { only.add(p); break; } }
            updateListAndRender(only); return;
        }
        if (selectedWard != null && !selectedWard.isEmpty()) {
            String target = norm(selectedWard);
            List<Post> wardPosts = new ArrayList<>();
            for (Post p : source) {
                if (p == null) continue;
                String wardFromPost = extractWard(p.getAddress());
                if (!norm(wardFromPost).isEmpty() && norm(wardFromPost).equals(target)) wardPosts.add(p);
            }
            updateListAndRender(wardPosts); return;
        }
        boolean isSearching = (postVM != null && postVM.isSearching());
        if (myLatLng != null && !isSearching) {
            List<Post> nearby = new ArrayList<>();
            for (Post p : source) {
                if (!isValidLatLng(p)) continue;
                float d = distanceMeters(myLatLng.getLatitude(), myLatLng.getLongitude(), p.getLat(), p.getLng());
                if (d <= RADIUS_METERS) nearby.add(p);
            }
            updateListAndRender(nearby); return;
        }
        updateListAndRender(source);
    }

    private void updateListAndRender(List<Post> list) {
        postList.clear(); postList.addAll(list);
        postAdapter.notifyDataSetChanged();
        renderMarkers(list);
        updateEmptyState(list.isEmpty());
        updateTitle();
    }

    private void updateEmptyState(boolean empty) {
        if (tvEmptyNearby == null) return;
        tvEmptyNearby.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderMarkers(List<Post> posts) {
        if (vietMapGL == null) return;
        for (Marker m : markers) { try { vietMapGL.removeMarker(m); } catch (Exception ignored) {} }
        markers.clear(); markerIdToPostId.clear();
        for (Post p : posts) {
            if (!isValidLatLng(p)) continue;
            String title = safe(p.getPostType()) + " - " + shortText(p.getDescription());
            String snippet = safe(p.getAddress());
            Marker m = vietMapGL.addMarker(new MarkerOptions().position(new LatLng(p.getLat(), p.getLng())).title(title).snippet(snippet));
            if (m != null) { markers.add(m); markerIdToPostId.put(m.getId(), p.getId()); }
        }
    }

    private String shortText(String s) { if (s == null) return ""; s = s.trim(); if (s.length() <= 20) return s; return s.substring(0, 20) + "..."; }
    private boolean isValidLatLng(Post p) { return p != null && !(p.getLat() == 0 && p.getLng() == 0) && p.getLat() >= -90 && p.getLat() <= 90 && p.getLng() >= -180 && p.getLng() <= 180; }
    private float distanceMeters(double lat1, double lon1, double lat2, double lon2) { float[] res = new float[1]; Location.distanceBetween(lat1, lon1, lat2, lon2, res); return res[0]; }
    private String safe(String s) { return s == null ? "" : s; }

    private void requestMyLocationAndRefresh() {
        if (!hasLocationPermission()) { requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION); myLatLng = null; forceNearbyTitle = false; updateTitle(); applyFilterAndRender(); return; }
        fetchLastLocationSafe();
    }

    @SuppressLint("MissingPermission")
    private void fetchLastLocationSafe() {
        try {
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) setMyLocation(loc);
                else { fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(cur -> { if (cur != null) setMyLocation(cur); else { myLatLng = null; forceNearbyTitle = false; updateTitle(); applyFilterAndRender(); } }).addOnFailureListener(e -> { myLatLng = null; forceNearbyTitle = false; updateTitle(); applyFilterAndRender(); }); }
            }).addOnFailureListener(e -> { myLatLng = null; forceNearbyTitle = false; updateTitle(); applyFilterAndRender(); });
        } catch (SecurityException se) { myLatLng = null; forceNearbyTitle = false; updateTitle(); applyFilterAndRender(); }
    }

    private void setMyLocation(@NonNull Location loc) {
        myLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        if (vietMapGL != null) vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
        forceNearbyTitle = false; updateTitle(); applyFilterAndRender();
    }

    private void moveToMyLocationAndFilter() {
        if (!hasLocationPermission()) { Toast.makeText(getContext(), "Chưa có quyền vị trí", Toast.LENGTH_SHORT).show(); requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION); return; }
        requestMyLocationAndRefresh();
    }

    @SuppressLint("MissingPermission")
    private void enableLocationComponent(@NonNull Style style) {
        if (vietMapGL == null) return;
        locationComponent = vietMapGL.getLocationComponent();
        if (locationComponent == null) return;
        if (!hasLocationPermission()) return;
        try {
            LocationComponentActivationOptions options = LocationComponentActivationOptions.builder(requireContext(), style).build();
            locationComponent.activateLocationComponent(options);
            setLocationEnabledCompat(locationComponent, true);
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS_NORTH);
            locationComponent.setRenderMode(RenderMode.GPS);
        } catch (SecurityException ignored) {}
    }

    private void setLocationEnabledCompat(@NonNull Object lc, boolean enabled) {
        try { Method m = lc.getClass().getMethod("setLocationComponentEnabled", boolean.class); m.invoke(lc, enabled); return; } catch (Exception ignored) {}
        try { Method m = lc.getClass().getDeclaredMethod("setLocationComponentEnabled", boolean.class); m.setAccessible(true); m.invoke(lc, enabled); return; } catch (Exception ignored) {}
        try { Method m = lc.getClass().getMethod("setIsLocationComponentEnabled", boolean.class); m.invoke(lc, enabled); return; } catch (Exception ignored) {}
        try { Field f = lc.getClass().getField("isLocationComponentEnabled"); f.setBoolean(lc, enabled); return; } catch (Exception ignored) {}
        try { Field f = lc.getClass().getDeclaredField("isLocationComponentEnabled"); f.setAccessible(true); f.setBoolean(lc, enabled); } catch (Exception ignored) {}
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = false; for (int r : grantResults) { if (r == PackageManager.PERMISSION_GRANTED) { granted = true; break; } }
            if (granted) { if (vietMapGL != null) { Style style = vietMapGL.getStyle(); if (style != null) enableLocationComponent(style); } requestMyLocationAndRefresh(); } else { Toast.makeText(getContext(), "Bạn chưa cấp quyền vị trí", Toast.LENGTH_SHORT).show(); myLatLng = null; forceNearbyTitle = false; updateTitle(); applyFilterAndRender(); }
        }
    }

    @Override public void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override public void onStop() { super.onStop(); if (mapView != null) mapView.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) { super.onSaveInstanceState(outState); if (mapView != null) mapView.onSaveInstanceState(outState); }
    @Override public void onDestroyView() {
        super.onDestroyView();
        if (pendingReverseCall != null) { pendingReverseCall.cancel(); pendingReverseCall = null; }
        if (vietMapGL != null) { for (Marker m : markers) { try { vietMapGL.removeMarker(m); } catch (Exception ignored) {} } }
        markers.clear(); markerIdToPostId.clear();
        if (mapView != null) { mapView.onDestroy(); mapView = null; }
        vietMapGL = null;
    }
}