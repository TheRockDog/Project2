package com.example.project2.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Category implements Parcelable {
    private int id;
    private String name;
    private List<String> packageNames;
    private int color;
    private boolean builtIn;

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
        this.packageNames = new ArrayList<>();
        this.color = 0xFF6200EE;
        this.builtIn = false;
    }

    protected Category(Parcel in) {
        id = in.readInt();
        name = in.readString();
        packageNames = in.createStringArrayList();
        color = in.readInt();
        builtIn = in.readByte() != 0;
    }

    public static final Creator<Category> CREATOR = new Creator<Category>() {
        @Override
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        @Override
        public Category[] newArray(int size) {
            return new Category[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeStringList(packageNames);
        dest.writeInt(color);
        dest.writeByte((byte) (builtIn ? 1 : 0));
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getPackageNames() { return packageNames; }
    public void setPackageNames(List<String> packageNames) { this.packageNames = packageNames; }
    public void addPackage(String packageName) {
        if (!packageNames.contains(packageName)) packageNames.add(packageName);
    }
    public void removePackage(String packageName) { packageNames.remove(packageName); }
    public boolean containsPackage(String packageName) { return packageNames.contains(packageName); }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
}