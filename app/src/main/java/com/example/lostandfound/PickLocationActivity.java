package com.example.lostandfound;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

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
import vn.vietmap.vietmapsdk.maps.MapView;
import vn.vietmap.vietmapsdk.maps.OnMapReadyCallback;
import vn.vietmap.vietmapsdk.maps.Style;
import vn.vietmap.vietmapsdk.maps.VietMapGL;

public class PickLocationActivity extends AppCompatActivity
        implements OnMapReadyCallback, VietMapGL.OnMapClickListener {

    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    public static final String EXTRA_LAT = "EXTRA_LAT";
    public static final String EXTRA_LNG = "EXTRA_LNG";

    private static final String STYLE_URL =
            "https://maps.vietmap.vn/api/maps/light/styles.json?apikey=f77a52c999a3400b244172210d4a0ebabb2f0c43926e1d45";

    private static final String SERVICES_KEY = "ba1cf0075ef140e2bccc1b2a4392454a11e042a82fb7674a";

    private MapView mapView;
    private VietMapGL vietMapGL;

    private TextView tvPickedAddress;
    private Button btnConfirm;
    private FloatingActionButton fabMyLocation;

    private Marker pickedMarker;
    private LatLng pickedLatLng;
    private String pickedAddress = "";

    private final OkHttpClient httpClient = new OkHttpClient();

    private final LatLng defaultPoint = new LatLng(10.8018, 106.7143);

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                if (fine || coarse) {
                    locateMe();
                } else {
                    Toast.makeText(this, "Bạn cần cho phép quyền vị trí để định vị", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Vietmap.getInstance(this);

        setContentView(R.layout.activity_pick_location);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvPickedAddress = findViewById(R.id.tv_picked_address);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        fabMyLocation = findViewById(R.id.fab_my_location);

        mapView = findViewById(R.id.pick_map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fabMyLocation.setOnClickListener(v -> locateMe());

        btnConfirm.setOnClickListener(v -> {
            if (pickedLatLng == null) {
                Toast.makeText(this, "Bạn hãy chạm lên bản đồ hoặc bấm định vị để chọn vị trí!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent data = new Intent();
            data.putExtra(EXTRA_ADDRESS, pickedAddress);
            data.putExtra(EXTRA_LAT, pickedLatLng.getLatitude());
            data.putExtra(EXTRA_LNG, pickedLatLng.getLongitude());

            setResult(RESULT_OK, data);
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull VietMapGL map) {
        this.vietMapGL = map;

        vietMapGL.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
            vietMapGL.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPoint, 15));

            vietMapGL.addOnMapClickListener(PickLocationActivity.this);

            tvPickedAddress.setText("Chạm lên bản đồ để chọn vị trí");
        });
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        pickPoint(point, true);
        return true;
    }

    private void pickPoint(@NonNull LatLng point, boolean animateCamera) {
        pickedLatLng = point;

        if (pickedMarker != null && vietMapGL != null) {
            try { vietMapGL.removeMarker(pickedMarker); } catch (Exception ignored) {}
        }

        if (vietMapGL != null) {
            pickedMarker = vietMapGL.addMarker(
                    new MarkerOptions().position(point).title("Vị trí đã chọn")
            );
            if (animateCamera) {
                vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17));
            }
        }

        pickedAddress = point.getLatitude() + ", " + point.getLongitude();
        tvPickedAddress.setText("Đang lấy địa chỉ...");

        reverseGeocode(point);
    }

    private void locateMe() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        tvPickedAddress.setText("Đang định vị...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        onGotLocation(loc);
                    } else {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener(current -> {
                                    if (current != null) onGotLocation(current);
                                    else tvPickedAddress.setText("Không lấy được vị trí hiện tại");
                                })
                                .addOnFailureListener(e -> tvPickedAddress.setText("Không lấy được vị trí hiện tại"));
                    }
                })
                .addOnFailureListener(e -> tvPickedAddress.setText("Không lấy được vị trí hiện tại"));
    }

    private void onGotLocation(@NonNull Location loc) {
        LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
        pickPoint(me, true);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    private void reverseGeocode(@NonNull LatLng point) {
        String url = "https://maps.vietmap.vn/api/reverse/v3"
                + "?apikey=" + SERVICES_KEY
                + "&lng=" + point.getLongitude()
                + "&lat=" + point.getLatitude();

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> tvPickedAddress.setText(pickedAddress));
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                String display = "";

                try {
                    JSONArray arr = new JSONArray(body);
                    if (arr.length() > 0) {
                        JSONObject obj = arr.getJSONObject(0);
                        display = obj.optString("display", "");
                    }
                } catch (Exception ignored) {}

                final String finalText = (display == null || display.trim().isEmpty())
                        ? pickedAddress
                        : display;

                runOnUiThread(() -> {
                    pickedAddress = finalText;
                    tvPickedAddress.setText(finalText);
                });
            }
        });
    }

    @Override protected void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); if (mapView != null) mapView.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override protected void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDestroy(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
