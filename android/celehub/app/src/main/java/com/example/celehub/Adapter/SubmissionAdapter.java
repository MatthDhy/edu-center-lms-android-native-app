package com.example.celehub.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.celehub.Model.GradeRow;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.R;
import com.example.celehub.Util.HomeFormat;
import com.example.celehub.Util.StatusChip;

import java.util.ArrayList;
import java.util.List;

/** Danh sách bài nộp ở các tab Chờ chấm và Đã chấm của giảng viên. */
public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.Holder> {

    public interface Listener {
        void onSubmissionClick(int position, GradeRow row);
    }

    private final List<GradeRow> items = new ArrayList<>();
    private final Listener listener;

    public SubmissionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<GradeRow> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        GradeRow row = items.get(position);
        SubmissionItem s = row.getSubmission();

        h.avatar.setText(HomeFormat.initials(s.getStudentName()));
        h.name.setText(s.getStudentName());
        h.sub.setText(row.getAssignmentTitle() + " • " + HomeFormat.shortDateTime(s.getSubmittedAt()));

        if (row.isGraded()) {
            h.score.setText(HomeFormat.score(s.getScore()));
            h.score.setTextColor(ContextCompat.getColor(h.score.getContext(), R.color.color_success));
            StatusChip.apply(h.chip, "Đã chấm", StatusChip.Variant.SUCCESS);
        } else {
            h.score.setText("/" + HomeFormat.score(row.getMaxScore()));
            h.score.setTextColor(ContextCompat.getColor(h.score.getContext(), R.color.color_text_muted));
            if (s.isLate()) {
                StatusChip.apply(h.chip, "Nộp trễ", StatusChip.Variant.WARNING);
            } else {
                StatusChip.apply(h.chip, "Chờ chấm", StatusChip.Variant.PRIMARY);
            }
        }

        h.card.setOnClickListener(v -> listener.onSubmissionClick(h.getBindingAdapterPosition(), row));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View card;
        final TextView avatar, name, sub, score, chip;

        Holder(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.cardSubmission);
            avatar = v.findViewById(R.id.tvAvatar);
            name = v.findViewById(R.id.tvName);
            sub = v.findViewById(R.id.tvSub);
            score = v.findViewById(R.id.tvScore);
            chip = v.findViewById(R.id.tvChip);
        }
    }
}