package com.example.celehub.Util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.R;

/** Chip trạng thái dạng viên thuốc, màu theo token. */
public final class StatusChip {

    private StatusChip() { }

    public enum Variant { PRIMARY, SUCCESS, WARNING, DANGER, NEUTRAL }

    public static void apply(TextView chip, String text, Variant variant) {
        Context c = chip.getContext();
        int bg;
        int fg;
        switch (variant) {
            case SUCCESS:
                bg = R.color.color_success_container;
                fg = R.color.color_success;
                break;
            case WARNING:
                bg = R.color.color_warning_container;
                fg = R.color.color_warning;
                break;
            case DANGER:
                bg = R.color.color_danger_container;
                fg = R.color.color_danger;
                break;
            case NEUTRAL:
                bg = R.color.color_outline;
                fg = R.color.color_text_muted;
                break;
            default:
                bg = R.color.color_primary_container;
                fg = R.color.color_primary;
        }
        chip.setText(text);
        chip.setBackgroundResource(R.drawable.bg_chip);
        chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, bg)));
        chip.setTextColor(ContextCompat.getColor(c, fg));
    }

    /** Chip trạng thái bài tập của học viên. */
    public static void forAssignment(TextView chip, AssignmentItem a) {
        String status = a.getStatus() == null ? "" : a.getStatus();
        switch (status) {
            case "SUBMITTED":
                apply(chip, "Đã nộp", Variant.SUCCESS);
                break;
            case "GRADED": {
                String text = "Đã chấm";
                SubmissionItem s = a.getMySubmission();
                if (s != null && s.getScore() != null) {
                    text += " • " + HomeFormat.score(s.getScore())
                            + "/" + HomeFormat.score(a.getMaxScoreOrDefault());
                }
                apply(chip, text, Variant.SUCCESS);
                break;
            }
            case "OVERDUE":
                apply(chip, "Quá hạn", Variant.DANGER);
                break;
            default:
                apply(chip, "Chưa nộp", Variant.PRIMARY);
        }
    }
}