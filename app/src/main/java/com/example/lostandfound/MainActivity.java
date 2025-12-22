package com.example.lostandfound;

import android.content.Intent; // Import để chuyển màn hình
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private ImageView btnNext;
    private TextView tvContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnNext = findViewById(R.id.btnNext);
        tvContinue = findViewById(R.id.tvContinue);

        if (btnNext != null && tvContinue != null) {

            View.OnClickListener continueListener = v -> navigateToNextScreen();

            btnNext.setOnClickListener(continueListener);
            tvContinue.setOnClickListener(continueListener);
        }
    }

    private void navigateToNextScreen() {
        Toast.makeText(this, "Clicked Continue!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}