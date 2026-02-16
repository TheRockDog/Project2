package com.example.project2;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

public class CreateCategoryDialog extends Dialog {
    private Context context;
    private CategoryManager categoryManager;
    private OnCategoryCreatedListener listener;

    public interface OnCategoryCreatedListener {
        void onCategoryCreated(Category category);
    }

    public CreateCategoryDialog(@NonNull Context context, OnCategoryCreatedListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.categoryManager = CategoryManager.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_create_category);

        EditText editName = findViewById(R.id.edit_category_name);
        Button btnCreate = findViewById(R.id.btn_create);
        Button btnCancel = findViewById(R.id.btn_cancel);

        btnCreate.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(context, "Введите название категории", Toast.LENGTH_SHORT).show();
                return;
            }

            Category category = categoryManager.createCategory(name);
            Toast.makeText(context, "Категория создана", Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onCategoryCreated(category);
            }

            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}