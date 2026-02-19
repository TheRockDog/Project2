package com.example.project2.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManager {
    private static final String PREFS_NAME = "category_prefs";
    private static final String KEY_CATEGORIES = "user_categories";
    private static final String KEY_APP_CATEGORIES = "app_categories";
    private static final int MAX_CATEGORIES = 5;

    private static CategoryManager instance;
    private Context context;
    private List<Category> categories;
    private Map<String, List<Integer>> appCategoryMap; // package -> category IDs
    private Gson gson;

    private CategoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.categories = new ArrayList<>();
        this.appCategoryMap = new HashMap<>();
        loadCategories();
    }

    public static synchronized CategoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryManager(context);
        }
        return instance;
    }

    private void loadCategories() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String categoriesJson = prefs.getString(KEY_CATEGORIES, "");
        if (!categoriesJson.isEmpty()) {
            try {
                Type type = new TypeToken<List<Category>>(){}.getType();
                List<Category> loadedCategories = gson.fromJson(categoriesJson, type);
                if (loadedCategories != null) {
                    categories = loadedCategories;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (categories.isEmpty()) {
            addDefaultCategory("Games", 0xFF4CAF50);
            addDefaultCategory("Social", 0xFF2196F3);
            addDefaultCategory("Work", 0xFFFF9800);
        }

        String appMapJson = prefs.getString(KEY_APP_CATEGORIES, "");
        if (!appMapJson.isEmpty()) {
            try {
                Type type = new TypeToken<Map<String, List<Integer>>>(){}.getType();
                Map<String, List<Integer>> loadedMap = gson.fromJson(appMapJson, type);
                if (loadedMap != null) {
                    appCategoryMap = loadedMap;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        syncCategoriesWithAppMap();
    }

    private void syncCategoriesWithAppMap() {
        for (Category category : categories) {
            category.getPackageNames().clear();
        }

        for (Map.Entry<String, List<Integer>> entry : appCategoryMap.entrySet()) {
            String packageName = entry.getKey();
            List<Integer> categoryIds = entry.getValue();

            for (Integer categoryId : categoryIds) {
                Category category = getCategory(categoryId);
                if (category != null) {
                    category.addPackage(packageName);
                }
            }
        }
    }

    private void saveCategories() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String categoriesJson = gson.toJson(categories);
        editor.putString(KEY_CATEGORIES, categoriesJson);

        String appMapJson = gson.toJson(appCategoryMap);
        editor.putString(KEY_APP_CATEGORIES, appMapJson);

        editor.apply();
    }

    private void addDefaultCategory(String name, int color) {
        Category category = new Category(categories.size(), name);
        category.setColor(color);
        categories.add(category);
    }

    public Category createCategory(String name) {
        if (categories.size() >= MAX_CATEGORIES) {
            return null;
        }
        int newId = categories.size();
        Category category = new Category(newId, name);
        categories.add(category);
        saveCategories();
        return category;
    }

    public void deleteCategory(int categoryId) {
        categories.removeIf(c -> c.getId() == categoryId);

        for (List<Integer> categoryIds : appCategoryMap.values()) {
            categoryIds.removeIf(id -> id == categoryId);
        }

        saveCategories();
    }

    public void updateCategory(Category category) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == category.getId()) {
                categories.set(i, category);
                break;
            }
        }
        saveCategories();
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categories);
    }

    public Category getCategory(int categoryId) {
        for (Category category : categories) {
            if (category.getId() == categoryId) {
                return category;
            }
        }
        return null;
    }

    public void addAppToCategory(String packageName, int categoryId) {
        List<Integer> categoryIds = appCategoryMap.get(packageName);
        if (categoryIds == null) {
            categoryIds = new ArrayList<>();
            appCategoryMap.put(packageName, categoryIds);
        }

        if (!categoryIds.contains(categoryId)) {
            categoryIds.add(categoryId);

            Category category = getCategory(categoryId);
            if (category != null) {
                category.addPackage(packageName);
            }

            saveCategories();
        }
    }

    public void removeAppFromCategory(String packageName, int categoryId) {
        List<Integer> categoryIds = appCategoryMap.get(packageName);
        if (categoryIds != null) {
            categoryIds.remove((Integer) categoryId);
            if (categoryIds.isEmpty()) {
                appCategoryMap.remove(packageName);
            }

            Category category = getCategory(categoryId);
            if (category != null) {
                category.removePackage(packageName);
            }

            saveCategories();
        }
    }

    public List<Integer> getAppCategories(String packageName) {
        List<Integer> categoryIds = appCategoryMap.get(packageName);
        return categoryIds != null ? new ArrayList<>(categoryIds) : new ArrayList<>();
    }

    public List<AppInfo> getAppsInCategory(Context context, int categoryId, List<AppInfo> allApps) {
        List<AppInfo> result = new ArrayList<>();
        Category category = getCategory(categoryId);

        if (category != null) {
            for (AppInfo app : allApps) {
                if (category.containsPackage(app.getPackageName())) {
                    result.add(app);
                }
            }
        }

        return result;
    }

    public void updateAppsWithUserCategories(List<AppInfo> apps) {
        for (AppInfo app : apps) {
            List<Integer> categoryIds = getAppCategories(app.getPackageName());
            app.setUserCategoryIds(categoryIds);
        }
    }

    public boolean isAppInUserCategory(String packageName, int categoryId) {
        List<Integer> categoryIds = appCategoryMap.get(packageName);
        return categoryIds != null && categoryIds.contains(categoryId);
    }

    public int getAppsCountInCategory(int categoryId) {
        Category category = getCategory(categoryId);
        if (category != null) {
            return category.getPackageNames().size();
        }
        return 0;
    }
}