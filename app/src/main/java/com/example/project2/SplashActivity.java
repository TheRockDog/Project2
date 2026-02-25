package com.example.project2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

        // Инициализация менеджера приложений
        AppManager.init(this);

        // Если кэш уже есть и актуален – запуск главной активности
        if (AppManager.isCacheValid()) {
            startMainActivity();
            return;
        }

        // Иначе запуск полного сканирования с отображением прогресса
        performInitialScan();
    }

    // Запуск главной активности и завершение текущей
    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Полное сканирование приложений с индикацией
    private void performInitialScan() {
        progressText.setText("Сканирование приложений...");
        progressBar.setProgress(0);

        // Получение списка приложений
        AppManager.refreshCacheAsync(this, new AppManager.AppLoadCallback() {
            @Override
            public void onLoaded(List<AppInfo> apps) {
                progressBar.setProgress(50);
                progressText.setText("Кэширование иконок...");

                // Загрузка иконок в кэш
                AppManager.loadIconsFromFilesAsync(SplashActivity.this, new AppManager.IconsLoadCallback() {
                    @Override
                    public void onIconsLoaded() {
                        progressBar.setProgress(100);
                        progressText.setText("Готово!");
                        // Небольшая задержка для визуального эффекта
                        new Handler(Looper.getMainLooper()).postDelayed(() -> startMainActivity(), 500);
                    }
                });
            }
        });
    }
}