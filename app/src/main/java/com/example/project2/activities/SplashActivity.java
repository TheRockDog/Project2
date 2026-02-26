package com.example.project2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project2.R;
import com.example.project2.models.AppInfo;
import com.example.project2.utils.AppManager;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);

        AppManager.init(this);

        if (AppManager.isCacheValid()) {
            startMainActivity();
            return;
        }

        performInitialScan();
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void performInitialScan() {
        progressText.setText("Сканирование приложений...");
        progressBar.setProgress(0);

        AppManager.refreshCacheAsync(this, new AppManager.AppLoadCallback() {
            @Override
            public void onLoaded(List<AppInfo> apps) {
                progressBar.setProgress(50);
                progressText.setText("Кэширование иконок...");

                AppManager.loadIconsFromFilesAsync(SplashActivity.this, new AppManager.IconsLoadCallback() {
                    @Override
                    public void onIconsLoaded() {
                        progressBar.setProgress(100);
                        progressText.setText("Готово!");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> startMainActivity(), 500);
                    }
                });
            }
        });
    }
}