package com.example.celehub.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.R;
import com.example.celehub.Util.HomeFormat;
import com.example.celehub.Util.StatusChip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.Holder> {

    public interface Listener {
        void onAssignmentClick(AssignmentItem assignment);
    }

    private final List<AssignmentItem> items = new ArrayList<>();
    private Map<Long, String> teachers = new HashMap<>();   // courseId -> tên giảng viên
    private final Listener listener;
    private final boolean teacherMode;

    public AssignmentAdapter(Listener listener) {
        this(false, listener);
    }
    public AssignmentAdapter(boolean teacherMode, Listener listener) {
        this.teacherMode = teacherMode;
        this.listener = listener;
    }

    public void submit(List<AssignmentItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    /** Tên giảng viên lấy từ danh sách khóa học, về sau cùng API bài tập. */
    public void setTeachers(Map<Long, String> map) {
        teachers = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AssignmentItem a = items.get(position);
        if (teacherMode) {
            h.title.setText(a.getTitle());
            h.course.setText(a.getCourseName());
            h.deadline.setText("Hạn nộp: " + HomeFormat.dateTime(a.getDeadline()));
            StatusChip.forTeacherAssignment(h.chip, a);

            long submitted = a.getSubmittedCount() == null ? 0 : a.getSubmittedCount();
            long graded = a.getGradedCount() == null ? 0 : a.getGradedCount();
            long total = a.getTotalStudents() == null ? 0 : a.getTotalStudents();
            String extra = submitted + "/" + total + " đã nộp";
            if (submitted - graded > 0) extra += " • " + (submitted - graded) + " chờ chấm";
            h.extra.setText(extra);
            h.extra.setVisibility(View.VISIBLE);

            h.card.setOnClickListener(v -> listener.onAssignmentClick(a));
            return;
        }

        h.title.setText(a.getTitle());

        String teacher = a.getCourseId() == null ? null : teachers.get(a.getCourseId());
        h.course.setText(teacher == null || teacher.isEmpty()
                ? a.getCourseName()
                : a.getCourseName() + " • GV: " + teacher);

        h.deadline.setText("Hạn nộp: " + HomeFormat.dateTime(a.getDeadline()));
        StatusChip.forAssignment(h.chip, a);

        String extra = "";
        SubmissionItem s = a.getMySubmission();
        if ((a.isSubmitted() || a.isGraded()) && s != null) {
            extra = "Nộp: " + HomeFormat.dateTime(s.getSubmittedAt()) + (s.isLate() ? " • Nộp muộn" : "");
        } else if (a.isOverdue()) {
            extra = "Vẫn có thể nộp muộn";
        }
        h.extra.setText(extra);
        h.extra.setVisibility(extra.isEmpty() ? View.GONE : View.VISIBLE);

        h.card.setOnClickListener(v -> listener.onAssignmentClick(a));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View card;
        final TextView title, course, deadline, chip, extra;

        Holder(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.cardAssignment);
            title = v.findViewById(R.id.tvAssignmentTitle);
            course = v.findViewById(R.id.tvAssignmentCourse);
            deadline = v.findViewById(R.id.tvAssignmentDeadline);
            chip = v.findViewById(R.id.tvAssignmentChip);
            extra = v.findViewById(R.id.tvAssignmentExtra);
        }
    }
}