package com.example.project2.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Category implements Serializable {
    private int id;
    private String name;
    private List<String> packageNames; // Пакеты приложений
    private int color;                   // Цвет категории
    private boolean builtIn;             // Встроенная категория (нельзя удалить/изменить)

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
        this.packageNames = new ArrayList<>();
        this.color = 0xFF6200EE; // Фиолетовый по умолчанию
        this.builtIn = false;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getPackageNames() { return packageNames; }
    public void setPackageNames(List<String> packageNames) { this.packageNames = packageNames; }

    // Добавляет пакет в категорию
    public void addPackage(String packageName) {
        if (!packageNames.contains(packageName)) {
            packageNames.add(packageName);
        }
    }

    // Удаляет пакет из категории
    public void removePackage(String packageName) {
        packageNames.remove(packageName);
    }

    // Проверяет, содержит ли категория пакет
    public boolean containsPackage(String packageName) {
        return packageNames.contains(packageName);
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
}