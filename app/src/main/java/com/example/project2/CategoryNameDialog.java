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

    public interface Listener { void onNameEntered(String name); }
    private Listener listener;

    public static CategoryNameDialog newInstance(Listener listener) {
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
        EditText editName = view.findViewById(R.id.edit_category_name);
        TextView errorText = view.findViewById(R.id.error_text);

        builder.setView(view)
                .setPositiveButton("Ок", null)
                .setNegativeButton("Отмена", (dialog, which) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String name = editName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("Название не может быть пустым");
                } else {
                    if (listener != null) listener.onNameEntered(name);
                    dialog.dismiss();
                }
            });
        });
        return dialog;
    }
}