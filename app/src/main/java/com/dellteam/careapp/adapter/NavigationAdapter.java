package com.dellteam.careapp.adapter;

import com.dellteam.careapp.Config;
import com.dellteam.careapp.fragment.WebFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

public class NavigationAdapter extends CacheFragmentStatePagerAdapter {

    private int mScrollY;

    private Fragment mCurrentFragment;

    public NavigationAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (getCurrentFragment() != object) {
            mCurrentFragment = ((Fragment) object);
        }
        super.setPrimaryItem(container, position, object);
    }

    public void setScrollY(int scrollY) {
        mScrollY = scrollY;
    }

    @Override
    protected Fragment createItem(int position) {
        // Initialize fragments.
        // Please be sure to pass scroll position to each fragments using setArguments.
        Fragment f;
        final int pattern = position % 3;
        f = WebFragment.newInstance(Config.URLS[position]);
        return f;
    }

    @Override
    public int getCount() {
        return Config.TITLES.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return Config.TITLES[position];
    }

    public Fragment getCurrentFragment() {
        return mCurrentFragment;
    }
}
