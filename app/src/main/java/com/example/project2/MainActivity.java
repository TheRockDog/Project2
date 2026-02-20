package com.example.project2;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.project2.adapters.CategoryAdapter;
import com.example.project2.models.Category;
import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements CategoryEditDialog.CategoryEditListener {

    private GridView gridView;
    private AppAdapter appAdapter;
    private CategoryAdapter categoryAdapter;
    private TabLayout tabLayout;
    private String currentCategory = "All";
    private boolean showingCategories = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.parody_title));

        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));

        gridView = findViewById(R.id.grid_view);
        tabLayout = findViewById(R.id.tab_layout);

        setupTabs();
        showAppsCategory("All");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem addItem = menu.findItem(R.id.action_add_category);
        if (addItem != null && addItem.getIcon() != null) {
            addItem.getIcon().setTint(ContextCompat.getColor(this, android.R.color.white));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_category) {
            // Открываем диалог ввода имени, затем сразу редактор без создания категории
            CategoryNameDialog.newInstance(name -> {
                // Передаём имя в диалог редактирования для создания новой категории
                CategoryEditDialog.newInstanceForCreate(name).show(getSupportFragmentManager(), "edit_category");
            }).show(getSupportFragmentManager(), "name_dialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Настройка вкладок
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Games"));
        tabLayout.addTab(tabLayout.newTab().setText("Social"));
        tabLayout.addTab(tabLayout.newTab().setText("Work"));
        tabLayout.addTab(tabLayout.newTab().setText("Other"));
        tabLayout.addTab(tabLayout.newTab().setText("Categories"));

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

    // Показ приложений авто-категории
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

    // Показ пользовательских категорий
    private void showCategories() {
        showingCategories = true;
        List<Category> allCategories = CategoryManager.getInstance(this).getAllCategories();
        List<Category> userCategories = allCategories.stream()
                .filter(c -> !c.isBuiltIn())
                .collect(Collectors.toList());
        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(this, userCategories,
                    category -> {},
                    (category) -> {
                        if (category.isBuiltIn()) {
                            Toast.makeText(this, R.string.builtin_category_no_edit, Toast.LENGTH_SHORT).show();
                        } else {
                            showCategoryOptionsDialog(category);
                        }
                    });
        } else {
            categoryAdapter.updateCategories(userCategories);
        }
        gridView.setAdapter(categoryAdapter);
    }

    // Диалог опций категории
    private void showCategoryOptionsDialog(Category category) {
        String[] options = {"Изменить", "Удалить"};
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

    // Подтверждение удаления
    private void showDeleteCategoryConfirmDialog(Category category) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete_category_title)
                .setMessage(getString(R.string.delete_category_confirm, category.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    CategoryManager.getInstance(this).deleteCategory(category.getId());
                    Toast.makeText(this, R.string.category_deleted, Toast.LENGTH_SHORT).show();
                    if (showingCategories) showCategories();
                    WidgetProvider.updateAllWidgets(this);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // Переключение на вкладку категорий
    public void switchToCategoriesTab() {
        TabLayout.Tab tab = tabLayout.getTabAt(5);
        if (tab != null) {
            tab.select();
        }
    }

    // Реализация интерфейса CategoryEditListener
    @Override
    public void onCategoryEdited() {
        if (showingCategories) {
            showCategories();
        }
    }
}