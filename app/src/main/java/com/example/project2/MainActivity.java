package com.example.project2;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.project2.AppAdapter;
import com.example.project2.adapters.CategoryAdapter;
import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;
    private AppAdapter appAdapter;
    private CategoryAdapter categoryAdapter;
    private TabLayout tabLayout;
    private String currentCategory = "All";
    private boolean showingCategories = false;
    private MaterialButton btnAddWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Пародия на Fences");

        gridView = findViewById(R.id.grid_view);
        tabLayout = findViewById(R.id.tab_layout);
        btnAddWidget = findViewById(R.id.btn_add_widget);

        setupTabs();

        btnAddWidget.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Чтобы добавить виджет: удерживайте пустое место на рабочем столе → Виджеты → Project2",
                    Toast.LENGTH_LONG).show();
        });

        showAppsCategory("All");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_category) {
            if (CategoryManager.getInstance(this).getAllCategories().size() >= 5) {
                Toast.makeText(this, "Максимум 5 категорий", Toast.LENGTH_SHORT).show();
                return true;
            }
            CategoryNameDialog.newInstance(name -> {
                Category category = CategoryManager.getInstance(this).createCategory(name);
                if (category == null) {
                    Toast.makeText(this, "Не удалось создать категорию", Toast.LENGTH_SHORT).show();
                    return;
                }
                CategoryEditDialog.newInstance(category).show(getSupportFragmentManager(), "edit_category");
            }).show(getSupportFragmentManager(), "name_dialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Все"));
        tabLayout.addTab(tabLayout.newTab().setText("Игры"));
        tabLayout.addTab(tabLayout.newTab().setText("Соцсети"));
        tabLayout.addTab(tabLayout.newTab().setText("Работа"));
        tabLayout.addTab(tabLayout.newTab().setText("Другое"));
        tabLayout.addTab(tabLayout.newTab().setText("Категории"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 5) {
                    showCategories();
                } else {
                    String category;
                    switch (tab.getPosition()) {
                        case 0: category = "All"; break;
                        case 1: category = "Games"; break;
                        case 2: category = "Social"; break;
                        case 3: category = "Work"; break;
                        default: category = "Other";
                    }
                    showAppsCategory(category);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showAppsCategory(String category) {
        showingCategories = false;
        currentCategory = category;
        AppManager.getAppsByCategoryAsync(this, category, apps -> {
            if (appAdapter == null) {
                appAdapter = new AppAdapter(MainActivity.this, apps);
            } else {
                appAdapter.updateApps(apps);
            }
            gridView.setAdapter(appAdapter);
        });
    }

    private void showCategories() {
        showingCategories = true;
        List<Category> categories = CategoryManager.getInstance(this).getAllCategories();
        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(this, categories,
                    category -> {
                        // Опционально: показать приложения этой категории
                    },
                    (category) -> {
                        showCategoryOptionsDialog(category);
                    });
        } else {
            categoryAdapter.updateCategories(categories);
        }
        gridView.setAdapter(categoryAdapter);
    }

    private void showCategoryOptionsDialog(Category category) {
        String[] options = {"✏️ Изменить", "🗑️ Удалить"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(category.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        CategoryEditDialog.newInstance(category).show(getSupportFragmentManager(), "edit_category");
                    } else {
                        showDeleteCategoryConfirmDialog(category);
                    }
                })
                .show();
    }

    private void showDeleteCategoryConfirmDialog(Category category) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удалить категорию")
                .setMessage("Вы уверены, что хотите удалить категорию \"" + category.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    CategoryManager.getInstance(this).deleteCategory(category.getId());
                    Toast.makeText(this, "🗑️ Категория удалена", Toast.LENGTH_SHORT).show();
                    if (showingCategories) showCategories();
                    WidgetProvider.updateAllWidgets(this);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    public void switchToCategoriesTab() {
        TabLayout.Tab tab = tabLayout.getTabAt(5);
        if (tab != null) {
            tab.select();
        }
    }
}