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
    private String autoCategory;          // Auto category
    private List<Integer> userCategoryIds; // User category IDs

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

    public void addToUserCategory(int categoryId) {
        if (!userCategoryIds.contains(categoryId)) {
            userCategoryIds.add(categoryId);
        }
    }

    public void removeFromUserCategory(int categoryId) {
        userCategoryIds.remove((Integer) categoryId);
    }

    public boolean isInUserCategory(int categoryId) {
        return userCategoryIds.contains(categoryId);
    }
}