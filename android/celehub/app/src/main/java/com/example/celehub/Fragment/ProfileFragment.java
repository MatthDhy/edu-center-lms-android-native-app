package com.example.celehub.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.celehub.API.ApiClient;
import com.example.celehub.Activity.LoginActivity;
import com.example.celehub.Data.SessionManager;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.UserDto;
import com.example.celehub.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private SessionManager session;
    private TextView tvInitials, tvName, tvMeta, tvEmail, tvPhone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        session = SessionManager.get(requireContext());

        tvInitials = view.findViewById(R.id.tvProfileInitials);
        tvName = view.findViewById(R.id.tvProfileName);
        tvMeta = view.findViewById(R.id.tvProfileMeta);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvPhone = view.findViewById(R.id.tvProfilePhone);

        applyStatusBarInset(view.findViewById(R.id.headerContainer));
        buildAccountGroup(view.findViewById(R.id.groupAccount));
        buildAppGroup(view.findViewById(R.id.groupApp));
        buildSupportGroup(view.findViewById(R.id.groupSupport));
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        // 1) Hiện ngay dữ liệu đã lưu lúc đăng nhập (không phải chờ mạng)
        bindUser();
        // 2) Rồi hỏi server lấy dữ liệu mới nhất (ví dụ admin vừa đổi số điện thoại)
        refreshFromServer();

        return view;
    }

    // ------------------------------------------------------------------
    // Dữ liệu người dùng thật
    // ------------------------------------------------------------------

    /** Đổ dữ liệu trong SessionManager lên khối tím. */
    private void bindUser() {
        tvInitials.setText(session.getInitials());
        tvName.setText(session.getFullName());
        tvMeta.setText(session.getDisplayCode() + " • " + session.getRoleLabel());
        tvEmail.setText(session.getEmail());

        String phone = session.getPhone();
        tvPhone.setText(phone.isEmpty() ? "Chưa cập nhật số điện thoại" : formatPhone(phone));
    }

    /** Gọi GET /api/users/me để làm mới thông tin. Lỗi mạng thì giữ nguyên dữ liệu cũ. */
    private void refreshFromServer() {
        ApiClient.get(requireContext()).getMe().enqueue(new Callback<ApiResponse<UserDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserDto>> call,
                                   Response<ApiResponse<UserDto>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    session.saveUser(response.body().getData());
                    bindUser();
                }
                // 401 đã được AuthInterceptor xử lý (tự đưa về Login), các lỗi khác bỏ qua
            }

            @Override
            public void onFailure(Call<ApiResponse<UserDto>> call, Throwable t) {
                // Mất mạng: vẫn hiện dữ liệu đã lưu
            }
        });
    }

    /** "0987654321" -> "0987 654 321" (đúng như Figma). Số khác độ dài thì giữ nguyên. */
    private static String formatPhone(String phone) {
        if (phone.length() == 10) {
            return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
        }
        return phone;
    }

    // ------------------------------------------------------------------
    // Các nhóm dòng
    // ------------------------------------------------------------------

    private void buildAccountGroup(LinearLayout group) {
        addRow(group, R.drawable.ic_nav_profile, "Thông tin cá nhân", null, true,
                () -> openChild(new EditProfileFragment()));
        addRow(group, R.drawable.ic_lock, "Đổi mật khẩu", null, true,
                () -> openChild(new ChangePasswordFragment()));
        addRow(group, R.drawable.ic_assessment, "Hồ sơ & Bảng điểm", null, true,
                () -> openChild(PlaceholderFragment.newInstance("Hồ sơ & Bảng điểm")));
    }

    private void buildAppGroup(LinearLayout group) {
        addSwitchRow(group, R.drawable.ic_dark_mode, "Giao diện tối", false);
        addRow(group, R.drawable.ic_language, "Ngôn ngữ", "Tiếng Việt", true,
                () -> toast("Ngôn ngữ"));
        addRow(group, R.drawable.ic_nav_notifications, "Thông báo", null, true,
                () -> toast("Thông báo"));
        addSwitchRow(group, R.drawable.ic_security, "Xác thực sinh trắc học", true);
    }

    private void buildSupportGroup(LinearLayout group) {
        addRow(group, R.drawable.ic_call, "Hotline", "1800 xxxx", false,
                () -> toast("Hotline"));
        addRow(group, R.drawable.ic_chat, "Zalo", null, true,
                () -> toast("Zalo"));
        addRow(group, R.drawable.ic_home_quiz, "FAQ", null, true,
                () -> toast("FAQ"));
        addRow(group, R.drawable.ic_description, "Điều khoản & Chính sách", null, true,
                () -> toast("Điều khoản & Chính sách"));
    }

    private void addRow(LinearLayout group, @DrawableRes int icon, String title,
                        @Nullable String value, boolean showChevron, Runnable onClick) {
        View row = inflateRow(group, icon, title);

        if (value != null) {
            TextView tvValue = row.findViewById(R.id.tvValue);
            tvValue.setText(value);
            tvValue.setVisibility(View.VISIBLE);
        }
        row.findViewById(R.id.ivChevron).setVisibility(showChevron ? View.VISIBLE : View.GONE);
        row.setOnClickListener(v -> onClick.run());
    }

    private void addSwitchRow(LinearLayout group, @DrawableRes int icon, String title,
                              boolean checked) {
        View row = inflateRow(group, icon, title);

        row.findViewById(R.id.ivChevron).setVisibility(View.GONE);
        SwitchCompat switchView = row.findViewById(R.id.switchView);
        switchView.setVisibility(View.VISIBLE);
        switchView.setChecked(checked);
        row.setOnClickListener(v -> switchView.toggle());
    }

    private View inflateRow(LinearLayout group, @DrawableRes int icon, String title) {
        View row = getLayoutInflater().inflate(R.layout.item_setting_row, group, false);
        ((ImageView) row.findViewById(R.id.ivIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.tvTitle)).setText(title);
        group.addView(row);
        return row;
    }

    // ------------------------------------------------------------------
    // Mở màn con và đăng xuất
    // ------------------------------------------------------------------

    /** Mở màn con trong vùng nội dung của MainActivity (thanh dưới vẫn còn). */
    private void openChild(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        // Backend dùng JWT không lưu phiên trên server nên chỉ cần xóa token ở máy
        session.clear();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        // Xóa toàn bộ các màn trước đó, để bấm Back không quay lại được vào app
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /** Đẩy khối tím xuống đúng bằng chiều cao thanh trạng thái (đồng hồ, pin...). */
    private void applyStatusBarInset(View header) {
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