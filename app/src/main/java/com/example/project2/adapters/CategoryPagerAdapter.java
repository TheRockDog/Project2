package com.example.project2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.project2.fragments.AppsFragment;

public class CategoryPagerAdapter extends FragmentStateAdapter {

    private final String[] categories = {"All", "Games", "Social", "Work", "Other"};

    public CategoryPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 5) {
            // Вкладка Categories – можно вернуть другой фрагмент или null, но лучше обработать отдельно
            // Пока оставим заглушку
            return new Fragment(); // Замените на фрагмент со списком категорий
        }
        return AppsFragment.newInstance(categories[position]);
    }

    @Override
    public int getItemCount() {
        return 6; // 5 категорий + Categories
    }
}