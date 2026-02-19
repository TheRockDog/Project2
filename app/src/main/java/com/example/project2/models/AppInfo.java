package com.example.project2.models;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class AppInfo {
    private String packageName;
    private String appName;
    private Drawable icon;
    private Bitmap cachedIcon;
    private String autoCategory;          // Авто-категория
    private List<Integer> userCategoryIds; // ID пользовательских категорий

    public AppInfo(String packageName, String appName, Drawable icon) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.autoCategory = "Other";
        this.userCategoryIds = new ArrayList<>();
    }

    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public Drawable getIcon() { return icon; }
    public Bitmap getCachedIcon() { return cachedIcon; }
    public void setCachedIcon(Bitmap cachedIcon) { this.cachedIcon = cachedIcon; }

    public String getAutoCategory() { return autoCategory; }
    public void setAutoCategory(String autoCategory) { this.autoCategory = autoCategory; }

    public List<Integer> getUserCategoryIds() { return userCategoryIds; }
    public void setUserCategoryIds(List<Integer> userCategoryIds) { this.userCategoryIds = userCategoryIds; }

    // Добавляет приложение в пользовательскую категорию
    public void addToUserCategory(int categoryId) {
        if (!userCategoryIds.contains(categoryId)) {
            userCategoryIds.add(categoryId);
        }
    }

    // Удаляет приложение из пользовательской категории
    public void removeFromUserCategory(int categoryId) {
        userCategoryIds.remove((Integer) categoryId);
    }

    // Проверяет, находится ли приложение в указанной категории
    public boolean isInUserCategory(int categoryId) {
        return userCategoryIds.contains(categoryId);
    }
}