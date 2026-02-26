package com.example.project2.fragments;

import android.os.Bundle;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project2.R;
import com.example.project2.adapters.CategoryRecyclerAdapter;
import com.example.project2.dialogs.CategoryEditDialog;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;
import com.example.project2.widget.WidgetProvider;

import java.util.List;
import java.util.stream.Collectors;

public class CategoriesFragment extends BaseListFragment<Category> {

    private CategoryManager categoryManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        categoryManager = CategoryManager.getInstance(requireContext());
    }

    @Override
    protected LiveData<List<Category>> getLiveData() {
        return categoryManager.getCategoriesLiveData();
    }

    @Override
    protected RecyclerView.Adapter createAdapter(List<Category> data) {
        return new CategoryRecyclerAdapter(requireContext(), data,
                category -> Toast.makeText(requireContext(), category.getName(), Toast.LENGTH_SHORT).show(),
                this::showCategoryOptionsDialog
        );
    }

    @Override
    protected List<Category> filterData(List<Category> data) {
        return data.stream()
                .filter(c -> !c.isBuiltIn())
                .collect(Collectors.toList());
    }

    @Override
    protected String getEmptyText() {
        return "Нет пользовательских категорий\nНажмите + чтобы создать";
    }

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

    private void showDeleteCategoryConfirmDialog(Category category) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_category_title)
                .setMessage(getString(R.string.delete_category_confirm, category.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    categoryManager.deleteCategory(category.getId());
                    Toast.makeText(requireContext(), R.string.category_deleted, Toast.LENGTH_SHORT).show();
                    WidgetProvider.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}