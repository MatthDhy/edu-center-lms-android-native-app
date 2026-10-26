package com.example.celehub.Activity;

import android.os.Bundle;
import android.graphics.Rect;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.celehub.Fragment.HomeFragment;
import com.example.celehub.Fragment.PlaceholderFragment;
import com.example.celehub.Fragment.ProfileFragment;
import com.example.celehub.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        // Ẩn thanh điều hướng khi bàn phím mở
        final View mainRoot = findViewById(R.id.mainRoot);
        mainRoot.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect visible = new Rect();
            mainRoot.getWindowVisibleDisplayFrame(visible);

            // 1) Bàn phím đang mở? (chiếm hơn 15% chiều cao màn hình)
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            boolean keyboardOpen = (screenHeight - visible.bottom) > screenHeight * 0.15;

            int navTarget = keyboardOpen ? View.GONE : View.VISIBLE;
            if (bottomNav.getVisibility() != navTarget) {
                bottomNav.setVisibility(navTarget);
            }

            // 2) Phần đáy của vùng nội dung đang bị bàn phím che bao nhiêu thì chừa bấy nhiêu
            int[] location = new int[2];
            mainRoot.getLocationOnScreen(location);
            int rootBottom = location[1] + mainRoot.getHeight();
            int padding = keyboardOpen ? Math.max(0, rootBottom - visible.bottom) : 0;

            if (mainRoot.getPaddingBottom() != padding) {
                mainRoot.setPadding(mainRoot.getPaddingLeft(), mainRoot.getPaddingTop(),
                        mainRoot.getPaddingRight(), padding);
            }
        });

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
            return new ProfileFragment();
        }
        return null;
    }
}
