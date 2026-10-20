package com.example.celehub.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.celehub.Fragment.HomeFragment;
import com.example.celehub.Fragment.PlaceholderFragment;
import com.example.celehub.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentFor(item.getItemId());
            if (fragment == null) return false;

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        });

        // Mở app lần đầu thì chọn sẵn tab Trang chủ
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /** Mỗi tab tương ứng một Fragment. Tab nào làm xong thì thay PlaceholderFragment bằng Fragment thật. */
    private Fragment fragmentFor(int menuId) {
        if (menuId == R.id.nav_home) {
            return new HomeFragment();
        } else if (menuId == R.id.nav_courses) {
            return PlaceholderFragment.newInstance("Khóa học");
        } else if (menuId == R.id.nav_assignments) {
            return PlaceholderFragment.newInstance("Bài tập");
        } else if (menuId == R.id.nav_notifications) {
            return PlaceholderFragment.newInstance("Thông báo");
        } else if (menuId == R.id.nav_profile) {
            return PlaceholderFragment.newInstance("Cá nhân");
        }
        return null;
    }
}
