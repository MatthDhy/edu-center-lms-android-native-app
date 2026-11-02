package com.example.celehub.Adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.celehub.Model.CourseItem;
import com.example.celehub.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.Holder> {

    public interface Listener {
        void onCourseClick(CourseItem course);
    }

    // {màu nền thẻ, màu chữ}. Lớp đã kết thúc luôn dùng màu xanh lá.
    private static final int[][] PALETTES = {
            {0xFFD1E4FF, 0xFF1E4F9E},   // xanh dương
            {0xFFFDE7A8, 0xFF8A5A00},   // vàng
            {0xFFE6DFFB, 0xFF3B2A78},   // tím
    };
    private static final int[] DONE = {0xFFCDEFD7, 0xFF1F7A3E};

    private final List<CourseItem> items = new ArrayList<>();
    private final Listener listener;

    public CourseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CourseItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CourseItem c = items.get(position);

        // Màu theo id lớp để lọc/tìm kiếm không làm đổi màu thẻ
        int idx = c.getId() == null ? 0 : (int) (c.getId() % PALETTES.length);
        int[] palette = c.isFinished() ? DONE : PALETTES[idx];
        int bg = palette[0];
        int fg = palette[1];

        h.card.setCardBackgroundColor(bg);
        h.name.setTextColor(fg);
        h.meta.setTextColor(fg);
        h.progressText.setTextColor(fg);
        h.progress.setIndicatorColor(fg);
        h.progress.setTrackColor((fg & 0x00FFFFFF) | 0x33000000);

        h.name.setText(c.getCourseName());

        StringBuilder meta = new StringBuilder();
        if (c.getTeacherName() != null && !c.getTeacherName().isEmpty()) {
            meta.append(c.getTeacherName()).append(" • ");
        }
        meta.append(c.getStudentCount()).append(" HV");
        long total = c.getTotalSessions();
        if (total > 0) meta.append(" • ").append(c.getHeldCount()).append("/").append(total).append(" buổi");
        h.meta.setText(meta.toString());

        h.progressText.setText(c.isFinished() ? "Đã kết thúc • 100%"
                : "Tiến độ " + c.getProgressPercent() + "%");
        h.progress.setProgress(c.getProgressPercent());

        h.card.setOnClickListener(v -> listener.onCourseClick(c));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView name, meta, progressText;
        final LinearProgressIndicator progress;

        Holder(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.cardCourse);
            name = v.findViewById(R.id.tvCourseName);
            meta = v.findViewById(R.id.tvCourseMeta);
            progressText = v.findViewById(R.id.tvCourseProgress);
            progress = v.findViewById(R.id.progressCourse);
        }
    }
}