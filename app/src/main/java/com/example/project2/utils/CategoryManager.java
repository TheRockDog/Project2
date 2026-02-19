package com.example.project2.utils;

import android.content.Context;
import android.content.SharedPreferences;

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
    private static final int MAX_CATEGORIES = 5;

    private static CategoryManager instance;
    private Context context;
    private List<Category> categories;
    private Map<String, List<Integer>> appCategoryMap; // package -> category IDs
    private int nextId; // Следующий свободный ID для пользовательских категорий
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

    // Загружает категории из SharedPreferences
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

        // Если список пуст, добавляем стандартные категории
        if (categories.isEmpty()) {
            addDefaultCategory("Games", 0xFF4CAF50, true);
            addDefaultCategory("Social", 0xFF2196F3, true);
            addDefaultCategory("Work", 0xFFFF9800, true);
        } else {
            // Для обратной совместимости: помечаем стандартные категории как builtIn
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
                if (loadedMap != null) {
                    appCategoryMap = loadedMap;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Загружаем следующий свободный ID
        nextId = prefs.getInt(KEY_NEXT_ID, 3); // По умолчанию 3 (после 0,1,2)
        int maxId = nextId - 1;
        for (Category cat : categories) {
            if (cat.getId() > maxId) maxId = cat.getId();
        }
        if (maxId >= nextId) {
            nextId = maxId + 1;
        }

        syncCategoriesWithAppMap();
        saveCategories();
    }

    // Синхронизирует список пакетов в категориях с appCategoryMap
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

    // Сохраняет категории и карту приложений в SharedPreferences
    private void saveCategories() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String categoriesJson = gson.toJson(categories);
        editor.putString(KEY_CATEGORIES, categoriesJson);

        String appMapJson = gson.toJson(appCategoryMap);
        editor.putString(KEY_APP_CATEGORIES, appMapJson);

        editor.putInt(KEY_NEXT_ID, nextId);

        editor.apply();
    }

    // Добавляет стандартную категорию
    private void addDefaultCategory(String name, int color, boolean builtIn) {
        Category category = new Category(categories.size(), name);
        category.setColor(color);
        category.setBuiltIn(builtIn);
        categories.add(category);
    }

    // Создаёт новую пользовательскую категорию
    public Category createCategory(String name) {
        if (categories.size() >= MAX_CATEGORIES) {
            return null;
        }
        Category category = new Category(nextId, name);
        category.setBuiltIn(false);
        categories.add(category);
        nextId++;
        saveCategories();
        return category;
    }

    // Удаляет категорию (только если не встроенная)
    public void deleteCategory(int categoryId) {
        Category cat = getCategory(categoryId);
        if (cat != null && cat.isBuiltIn()) {
            return;
        }
        categories.removeIf(c -> c.getId() == categoryId);

        for (List<Integer> categoryIds : appCategoryMap.values()) {
            categoryIds.removeIf(id -> id == categoryId);
        }

        saveCategories();
    }

    // Обновляет категорию
    public void updateCategory(Category category) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == category.getId()) {
                categories.set(i, category);
                break;
            }
        }
        saveCategories();
    }

    // Возвращает все категории
    public List<Category> getAllCategories() {
        return new ArrayList<>(categories);
    }

    // Возвращает категорию по ID
    public Category getCategory(int categoryId) {
        for (Category category : categories) {
            if (category.getId() == categoryId) {
                return category;
            }
        }
        return null;
    }

    // Добавляет приложение в категорию
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

    // Удаляет приложение из категории
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

    // Возвращает список ID категорий для приложения
    public List<Integer> getAppCategories(String packageName) {
        List<Integer> categoryIds = appCategoryMap.get(packageName);
        return categoryIds != null ? new ArrayList<>(categoryIds) : new ArrayList<>();
    }

    // Обновляет список приложений, добавляя информацию о пользовательских категориях
    public void updateAppsWithUserCategories(List<AppInfo> apps) {
        for (AppInfo app : apps) {
            List<Integer> categoryIds = getAppCategories(app.getPackageName());
            app.setUserCategoryIds(categoryIds);
        }
    }

    // Возвращает количество приложений в категории
    public int getAppsCountInCategory(int categoryId) {
        Category category = getCategory(categoryId);
        if (category != null) {
            return category.getPackageNames().size();
        }
        return 0;
    }
}