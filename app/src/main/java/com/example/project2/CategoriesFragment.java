package com.example.project2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project2.adapters.CategoryAdapter;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.List;
import java.util.stream.Collectors;

public class CategoriesFragment extends Fragment {

    private GridView gridView;                      // Сетка категорий
    private CategoryAdapter categoryAdapter;        // Адаптер категорий
    private CategoryManager categoryManager;        // Менеджер категорий

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);
        gridView = view.findViewById(R.id.grid_view);
        categoryManager = CategoryManager.getInstance(requireContext());
        loadCategories();
        return view;
    }

    // Загрузка пользовательских категорий
    private void loadCategories() {
        List<Category> allCategories = categoryManager.getAllCategories();
        List<Category> userCategories = allCategories.stream()
                .filter(c -> !c.isBuiltIn())
                .collect(Collectors.toList());

        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(requireContext(), userCategories,
                    category -> Toast.makeText(requireContext(), category.getName(), Toast.LENGTH_SHORT).show(),
                    this::showCategoryOptionsDialog
            );
            gridView.setAdapter(categoryAdapter);
        } else {
            categoryAdapter.updateCategories(userCategories);
        }
    }

    // Диалог опций категории
    private void showCategoryOptionsDialog(Category category) {
        String[] options = {"Изменить", "Удалить"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(category.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        CategoryEditDialog.newInstance(category)
                                .show(getParentFragmentManager(), "edit_category");
                    } else {
                        showDeleteCategoryConfirmDialog(category);
                    }
                })
                .show();
    }

    // Подтверждение удаления
    private void showDeleteCategoryConfirmDialog(Category category) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_category_title)
                .setMessage(getString(R.string.delete_category_confirm, category.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    categoryManager.deleteCategory(category.getId());
                    Toast.makeText(requireContext(), R.string.category_deleted, Toast.LENGTH_SHORT).show();
                    refreshCategories();
                    WidgetProvider.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // Обновление списка категорий
    public void refreshCategories() {
        loadCategories();
    }
}