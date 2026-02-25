package com.example.project2.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
    private static final String KEY_NEXT_ID = "next_category_id";

    private static CategoryManager instance;
    private Context context;
    private List<Category> categories;
    private Map<String, List<Integer>> appCategoryMap;
    private int nextId;
    private Gson gson;

    private MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>(); // Для наблюдения

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

    // Возвращает LiveData для наблюдения
    public LiveData<List<Category>> getCategoriesLiveData() {
        return categoriesLiveData;
    }

    private void loadCategories() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String categoriesJson = prefs.getString(KEY_CATEGORIES, "");
        if (!categoriesJson.isEmpty()) {
            try {
                Type type = new TypeToken<List<Category>>(){}.getType();
                List<Category> loadedCategories = gson.fromJson(categoriesJson, type);
                if (loadedCategories != null) categories = loadedCategories;
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (categories.isEmpty()) {
            addDefaultCategory("Games", 0xFF4CAF50, true);
            addDefaultCategory("Social", 0xFF2196F3, true);
            addDefaultCategory("Work", 0xFFFF9800, true);
        } else {
            for (Category cat : categories) {
                if (cat.getId() < 3 && ("Games".equals(cat.getName()) || "Social".equals(cat.getName()) || "Work".equals(cat.getName()))) {
                    cat.setBuiltIn(true);
                }
            }
        }

        String appMapJson = prefs.getString(KEY_APP_CATEGORIES, "");
        if (!appMapJson.isEmpty()) {
            try {
                Type type = new TypeToken<Map<String, List<Integer>>>(){}.getType();
                Map<String, List<Integer>> loadedMap = gson.fromJson(appMapJson, type);
                if (loadedMap != null) appCategoryMap = loadedMap;
            } catch (Exception e) { e.printStackTrace(); }
        }

        nextId = prefs.getInt(KEY_NEXT_ID, 3);
        int maxId = nextId - 1;
        for (Category cat : categories) if (cat.getId() > maxId) maxId = cat.getId();
        if (maxId >= nextId) nextId = maxId + 1;

        syncCategoriesWithAppMap();
        saveCategories();
        categoriesLiveData.setValue(new ArrayList<>(categories));
    }

    private void syncCategoriesWithAppMap() {
        for (Category cat : categories) cat.getPackageNames().clear();
        for (Map.Entry<String, List<Integer>> entry : appCategoryMap.entrySet()) {
            String pkg = entry.getKey();
            for (Integer catId : entry.getValue()) {
                Category cat = getCategory(catId);
                if (cat != null) cat.addPackage(pkg);
            }
        }
    }

    private void saveCategories() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CATEGORIES, gson.toJson(categories))
                .putString(KEY_APP_CATEGORIES, gson.toJson(appCategoryMap))
                .putInt(KEY_NEXT_ID, nextId)
                .apply();
        categoriesLiveData.setValue(new ArrayList<>(categories));
    }

    private void addDefaultCategory(String name, int color, boolean builtIn) {
        Category cat = new Category(categories.size(), name);
        cat.setColor(color);
        cat.setBuiltIn(builtIn);
        categories.add(cat);
    }

    public Category createCategory(String name) {
        Category cat = new Category(nextId, name);
        cat.setBuiltIn(false);
        categories.add(cat);
        nextId++;
        saveCategories();
        return cat;
    }

    public void deleteCategory(int categoryId) {
        Category cat = getCategory(categoryId);
        if (cat != null && cat.isBuiltIn()) return;
        categories.removeIf(c -> c.getId() == categoryId);
        for (List<Integer> ids : appCategoryMap.values()) ids.removeIf(id -> id == categoryId);
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
        for (Category cat : categories) if (cat.getId() == categoryId) return cat;
        return null;
    }

    public void addAppToCategory(String packageName, int categoryId) {
        List<Integer> ids = appCategoryMap.get(packageName);
        if (ids == null) { ids = new ArrayList<>(); appCategoryMap.put(packageName, ids); }
        if (!ids.contains(categoryId)) {
            ids.add(categoryId);
            Category cat = getCategory(categoryId);
            if (cat != null) cat.addPackage(packageName);
            saveCategories();
        }
    }

    public void removeAppFromCategory(String packageName, int categoryId) {
        List<Integer> ids = appCategoryMap.get(packageName);
        if (ids != null) {
            ids.remove((Integer) categoryId);
            if (ids.isEmpty()) appCategoryMap.remove(packageName);
            Category cat = getCategory(categoryId);
            if (cat != null) cat.removePackage(packageName);
            saveCategories();
        }
    }

    public List<Integer> getAppCategories(String packageName) {
        List<Integer> ids = appCategoryMap.get(packageName);
        return ids != null ? new ArrayList<>(ids) : new ArrayList<>();
    }

    public void updateAppsWithUserCategories(List<AppInfo> apps) {
        for (AppInfo app : apps) {
            app.setUserCategoryIds(getAppCategories(app.getPackageName()));
        }
    }

    public int getAppsCountInCategory(int categoryId) {
        Category cat = getCategory(categoryId);
        return cat != null ? cat.getPackageNames().size() : 0;
    }
}