package com.example.celehub.Util;

import android.text.Editable;
import android.text.TextWatcher;

/** TextWatcher rút gọn: chỉ cần viết onChanged() thay vì cả 3 hàm. */
public abstract class SimpleTextWatcher implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        onChanged(s.toString());
    }

    public abstract void onChanged(String text);
}