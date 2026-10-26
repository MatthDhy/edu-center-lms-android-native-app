package com.example.celehub.Util;

import com.google.android.material.textfield.TextInputLayout;

/** Tiện ích cho các ô nhập TextInputLayout. */
public final class FormErrors {

    private FormErrors() { }

    /**
     * Xóa lỗi và THU HẸP luôn dòng chữ lỗi.
     *
     * Chỉ gọi setError(null) thì chữ đỏ mất nhưng TextInputLayout vẫn giữ chỗ trống của dòng lỗi,
     * nên các thành phần bên dưới (ví dụ nút Lưu) không trở lại vị trí cũ.
     * setErrorEnabled(false) mới trả lại khoảng trống đó.
     */
    public static void clear(TextInputLayout layout) {
        if (layout.isErrorEnabled()) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }
}