package com.example.project2;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SectionsPagerAdapter extends FragmentStateAdapter {

    private static final String[] TAB_TITLES = new String[]{
            "All", "Games", "Social", "Work", "Other", "Categories"
    };

    public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 5) {
            return new CategoriesFragment();
        } else {
            return AppListFragment.newInstance(TAB_TITLES[position]);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_TITLES.length;
    }

    public String getPageTitle(int position) {
        return TAB_TITLES[position];
    }

    public int getCategoryTabPosition() {
        return 5;
    }
}