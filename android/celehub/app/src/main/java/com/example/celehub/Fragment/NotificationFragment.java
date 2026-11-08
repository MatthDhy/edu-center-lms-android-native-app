package com.example.celehub.Fragment;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.NotificationItem;
import com.example.celehub.Model.NotificationPage;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.HomeFormat;

/**
 * Danh sách thông báo, mới nhất ở trên. Thông báo chưa đọc có nền tím nhạt, chấm tròn và chữ đậm;
 * bấm vào hoặc "Đọc tất cả" thì chỉ bỏ phần nổi bật đó, thông báo vẫn ở lại trong danh sách.
 */
public class NotificationFragment extends Fragment {

    private static final int PAGE_SIZE = 20;

    private final List<NotificationItem> items = new ArrayList<>();
    private int nextPage = 0;
    private int totalPages = 1;
    private boolean loading;
    private boolean markingAll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        ChildHeader.bind(this, view, "Thông báo");

        view.findViewById(R.id.btnReadAll).setOnClickListener(v -> readAll());
        view.findViewById(R.id.btnLoadMore).setOnClickListener(v -> loadPage());

        loadPage();
        return view;
    }

    // ===================== Tải dữ liệu =====================

    private void loadPage() {
        if (loading) return;
        loading = true;
        render();

        ApiClient.get(requireContext()).getNotifications(nextPage, PAGE_SIZE)
                .enqueue(new Callback<ApiResponse<NotificationPage>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<NotificationPage>> call,
                                           Response<ApiResponse<NotificationPage>> res) {
                        loading = false;
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            NotificationPage p = res.body().getData();
                            if (p.getContent() != null) items.addAll(p.getContent());
                            nextPage = p.getPage() + 1;
                            totalPages = p.getTotalPages();
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được thông báo"),
                                    Toast.LENGTH_SHORT).show();
                        }
                        render();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<NotificationPage>> call, Throwable t) {
                        loading = false;
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                        render();
                    }
                });
    }

    // ===================== Vẽ giao diện =====================

    private void render() {
        View v = getView();
        if (v == null) return;

        LinearLayout list = v.findViewById(R.id.llList);
        TextView empty = v.findViewById(R.id.tvEmpty);
        list.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (NotificationItem n : items) {
            View row = inflater.inflate(R.layout.item_notification, list, false);
            ((TextView) row.findViewById(R.id.tvTitle)).setText(n.getTitle());
            TextView content = row.findViewById(R.id.tvContent);
            boolean hasContent = n.getContent() != null && !n.getContent().trim().isEmpty();
            content.setVisibility(hasContent ? View.VISIBLE : View.GONE);
            if (hasContent) content.setText(n.getContent());
            ((TextView) row.findViewById(R.id.tvTime)).setText(HomeFormat.ago(n.getCreatedAt()));

            styleRow(row, n);
            row.setOnClickListener(x -> {
                if (!n.isRead()) markRead(n, row);
            });
            list.addView(row);
        }

        if (items.isEmpty()) {
            empty.setText(loading ? "Đang tải..." : "Chưa có thông báo nào.");
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
        v.findViewById(R.id.btnLoadMore).setVisibility(
                !loading && nextPage < totalPages ? View.VISIBLE : View.GONE);
        updateHeader(v);
    }

    /** Chưa đọc: nền tím nhạt + chấm + chữ đậm. Đã đọc: nền trắng, chữ thường, không chấm. */
    private void styleRow(View row, NotificationItem n) {
        boolean read = n.isRead();
        ((MaterialCardView) row).setCardBackgroundColor(read
                ? Color.WHITE : requireContext().getColor(R.color.color_primary_container));
        row.findViewById(R.id.dotUnread).setVisibility(read ? View.INVISIBLE : View.VISIBLE);

        TextView title = row.findViewById(R.id.tvTitle);
        title.setTypeface(title.getTypeface(), read ? Typeface.NORMAL : Typeface.BOLD);
        title.setTextColor(requireContext().getColor(
                read ? R.color.color_text_secondary : R.color.color_on_surface));
    }

    private void updateHeader(View v) {
        int unread = 0;
        for (NotificationItem n : items) if (!n.isRead()) unread++;
        boolean more = nextPage < totalPages;

        TextView summary = v.findViewById(R.id.tvSummary);
        if (items.isEmpty()) {
            summary.setText("");
        } else if (unread > 0) {
            summary.setText(unread + (more ? "+" : "") + " thông báo chưa đọc");
        } else {
            summary.setText(more ? "" : "Bạn đã đọc hết thông báo");
        }
        v.findViewById(R.id.btnReadAll).setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
    }

    // ===================== Đánh dấu đã đọc =====================

    private void markRead(NotificationItem n, View row) {
        ApiClient.get(requireContext()).markNotificationRead(n.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> res) {
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful()) {
                            n.markRead();
                            styleRow(row, n);          // chỉ đổi dòng này, danh sách không nhảy
                            updateHeader(getView());
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không thể đánh dấu đã đọc"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void readAll() {
        if (markingAll) return;
        markingAll = true;
        ApiClient.get(requireContext()).markAllNotificationsRead()
                .enqueue(new Callback<ApiResponse<Map<String, Integer>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Integer>>> call,
                                           Response<ApiResponse<Map<String, Integer>>> res) {
                        markingAll = false;
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful()) {
                            for (NotificationItem n : items) n.markRead();
                            render();
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không thể đánh dấu đã đọc"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Integer>>> call, Throwable t) {
                        markingAll = false;
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}