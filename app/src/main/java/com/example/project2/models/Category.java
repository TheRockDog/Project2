package com.example.project2.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Category implements Serializable {
    private int id;
    private String name;
    private List<String> packageNames; // Список пакетов приложений в категории
    private int iconResId; // Для иконки категории (опционально)
    private int color; // Цвет категории для виджета

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
        this.packageNames = new ArrayList<>();
        this.color = 0xFF6200EE; // Фиолетовый по умолчанию
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getPackageNames() { return packageNames; }
    public void setPackageNames(List<String> packageNames) { this.packageNames = packageNames; }

    public void addPackage(String packageName) {
        if (!packageNames.contains(packageName)) {
            packageNames.add(packageName);
        }
    }

    public void removePackage(String packageName) {
        packageNames.remove(packageName);
    }

    public boolean containsPackage(String packageName) {
        return packageNames.contains(packageName);
    }

    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}