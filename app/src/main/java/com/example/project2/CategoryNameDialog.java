package com.example.project2;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class CategoryNameDialog extends DialogFragment {

    public interface CategoryNameListener {
        void onCategoryNameEntered(String name);
    }

    private CategoryNameListener listener;

    public static CategoryNameDialog newInstance(CategoryNameListener listener) {
        CategoryNameDialog dialog = new CategoryNameDialog();
        dialog.listener = listener;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_category_name, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_category_name);
        TextView errorText = view.findViewById(R.id.error_text);
        Button btnOk = view.findViewById(R.id.btn_ok);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        AlertDialog dialog = builder.create();

        btnOk.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Название не может быть пустым");
            } else {
                if (listener != null) {
                    listener.onCategoryNameEntered(name);
                }
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }
}