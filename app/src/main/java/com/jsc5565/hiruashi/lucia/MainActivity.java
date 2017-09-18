package com.jsc5565.hiruashi.lucia;

import android.Manifest;
import android.os.Bundle;
import android.support.design.widget.TabLayout;

import hiruashi.jsc5565.packingproject.Packing.PackPermissionActivity;
import hiruashi.jsc5565.packingproject.Packing.PackViewPager;

public class MainActivity extends PackPermissionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
        setFinish(false);
        runPermission();

        TabLayout tabLayout = (TabLayout)findViewById(R.id.tab_layout);

        final PackViewPager viewPager = (PackViewPager)findViewById(R.id.viewpager);

        viewPager.addFragments(new CalendarFragment(), new GallaryFragment(), new VideoFragment());
        viewPager.addFragmentTitles("Schedule", "Gallery", "Video");
        viewPager.setOffscreenPageLimit(3);

        tabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

}
