package com.example.project2.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project2.R;
import com.example.project2.fragments.BaseListFragment;
import com.example.project2.models.AppInfo;
import com.example.project2.utils.AppManager;

import java.util.List;

public class AppRecyclerAdapter extends RecyclerView.Adapter<AppRecyclerAdapter.ViewHolder>
        implements BaseListFragment.UpdatableAdapter<AppInfo> {

    public interface OnAppLongClickListener {
        void onLongClick(AppInfo app);
    }

    private final Context context;
    private List<AppInfo> apps;
    private final PackageManager packageManager;
    private final OnAppLongClickListener longClickListener;

    public AppRecyclerAdapter(Context context, List<AppInfo> apps, OnAppLongClickListener listener) {
        this.context = context;
        this.apps = apps;
        this.packageManager = context.getPackageManager();
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.name.setText(app.getAppName());
        holder.icon.setImageDrawable(app.getIcon());

        loadIconAsync(app, holder.icon);

        holder.itemView.setOnClickListener(v -> launchApp(app));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    @Override
    public void updateData(List<AppInfo> newData) {
        this.apps = newData;
        notifyDataSetChanged();
    }

    private void loadIconAsync(AppInfo app, ImageView imageView) {
        Bitmap cached = AppManager.getCachedIcon(app.getPackageName());
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }
        imageView.setTag(app.getPackageName());
        AppManager.executor.execute(() -> {
            Bitmap bitmap = AppManager.loadIconBitmap(context, app.getPackageName());
            if (bitmap != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (imageView.getTag() != null && imageView.getTag().equals(app.getPackageName())) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private void launchApp(AppInfo app) {
        try {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(app.getPackageName());
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            } else {
                Toast.makeText(context, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
        }
    }
}