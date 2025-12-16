package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    // ✅ KEY TRẢ VỀ (PostActivity đang đọc đúng các key này)
    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    public static final String EXTRA_LAT = "EXTRA_LAT";
    public static final String EXTRA_LNG = "EXTRA_LNG";

    // Tilemap key: dùng cho hiển thị bản đồ
    private static final String STYLE_URL =
            "https://maps.vietmap.vn/api/maps/light/styles.json?apikey=f77a52c999a3400b244172210d4a0ebabb2f0c43926e1d45";

    // Services key: dùng cho reverse geocode lấy địa chỉ
    private static final String SERVICES_KEY = "ba1cf0075ef140e2bccc1b2a4392454a11e042a82fb7674a";

    private MapView mapView;
    private VietMapGL vietMapGL;

    private TextView tvPickedAddress;
    private Button btnConfirm;

    private Marker pickedMarker;
    private LatLng pickedLatLng;
    private String pickedAddress = "";

    private final OkHttpClient httpClient = new OkHttpClient();

    private final LatLng defaultPoint = new LatLng(10.8018, 106.7143);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ QUAN TRỌNG: init Vietmap trước khi inflate layout có MapView
        Vietmap.getInstance(this);

        setContentView(R.layout.activity_pick_location);

        tvPickedAddress = findViewById(R.id.tv_picked_address);
        btnConfirm = findViewById(R.id.btn_confirm_location);

        mapView = findViewById(R.id.pick_map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        btnConfirm.setOnClickListener(v -> {
            if (pickedLatLng == null) {
                Toast.makeText(this, "Bạn hãy chạm lên bản đồ để chọn vị trí!", Toast.LENGTH_SHORT).show();
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

            // click map để chọn vị trí
            vietMapGL.addOnMapClickListener(PickLocationActivity.this);

            tvPickedAddress.setText("Chạm lên bản đồ để chọn vị trí");
        });
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        pickedLatLng = point;

        // remove marker cũ
        if (pickedMarker != null && vietMapGL != null) {
            try { vietMapGL.removeMarker(pickedMarker); } catch (Exception ignored) {}
        }

        // add marker mới
        if (vietMapGL != null) {
            pickedMarker = vietMapGL.addMarker(
                    new MarkerOptions().position(point).title("Vị trí đã chọn")
            );
            vietMapGL.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17));
        }

        // fallback trước: tọa độ
        pickedAddress = point.getLatitude() + ", " + point.getLongitude();
        tvPickedAddress.setText("Đang lấy địa chỉ...");

        // gọi reverse geocode để ra địa chỉ chữ
        reverseGeocode(point);

        return true;
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

    // ===== MapView lifecycle =====
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
