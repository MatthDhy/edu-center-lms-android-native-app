package com.example.celehub.Util;

import android.view.View;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.celehub.R;

/** Gắn tiêu đề, nút quay lại và chừa chỗ cho thanh trạng thái của header dùng chung (layout_child_header). */
public final class ChildHeader {

    private ChildHeader() { }

    public static void bind(Fragment fragment, View root, String title) {
        ((TextView) root.findViewById(R.id.tvHeaderTitle)).setText(title);

        // Nút quay lại: bỏ màn con trên cùng ra khỏi back stack
        root.findViewById(R.id.btnBack).setOnClickListener(v ->
                fragment.getParentFragmentManager().popBackStack());

        // Đẩy header xuống đúng bằng chiều cao thanh trạng thái
        View header = root.findViewById(R.id.childHeader);
        final int basePaddingTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), basePaddingTop + statusBarHeight,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }
}