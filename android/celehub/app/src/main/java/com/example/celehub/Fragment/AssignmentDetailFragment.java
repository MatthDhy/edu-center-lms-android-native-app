package com.example.celehub.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FileUtils;
import com.example.celehub.Util.HomeFormat;
import com.example.celehub.Util.StatusChip;

/** Học viên xem chi tiết bài tập. Từ đây bấm nút dưới cùng để sang màn nộp bài. */
public class AssignmentDetailFragment extends Fragment {

    private static final String ARG_ID = "assignment_id";

    private long assignmentId;

    public static AssignmentDetailFragment newInstance(long assignmentId) {
        AssignmentDetailFragment f = new AssignmentDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, assignmentId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assignment_detail, container, false);
        assignmentId = requireArguments().getLong(ARG_ID);
        ChildHeader.bind(this, view, "Chi tiết bài tập");
        load();   // quay lại từ màn nộp bài thì view được tạo lại nên trạng thái luôn mới
        return view;
    }

    private void load() {
        ApiClient.get(requireContext()).getAssignment(assignmentId)
                .enqueue(new Callback<ApiResponse<AssignmentItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AssignmentItem>> call,
                                           Response<ApiResponse<AssignmentItem>> res) {
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            bind(res.body().getData());
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được bài tập"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AssignmentItem>> call, Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bind(AssignmentItem a) {
        View v = getView();
        if (v == null) return;

        text(v, R.id.tvTitle).setText(a.getTitle());
        StatusChip.forAssignment(text(v, R.id.tvStatusChip), a);
        text(v, R.id.tvCourse).setText(a.getCourseName());
        text(v, R.id.tvDeadline).setText("Hạn nộp: " + HomeFormat.dateTime(a.getDeadline()));

        TextView teacher = text(v, R.id.tvTeacher);
        boolean hasTeacher = a.getTeacherName() != null && !a.getTeacherName().isEmpty();
        teacher.setVisibility(hasTeacher ? View.VISIBLE : View.GONE);
        if (hasTeacher) teacher.setText("Giáo viên giao bài: " + a.getTeacherName());

        // Mô tả
        boolean hasDesc = a.getDescription() != null && !a.getDescription().trim().isEmpty();
        v.findViewById(R.id.cardDescription).setVisibility(hasDesc ? View.VISIBLE : View.GONE);
        if (hasDesc) text(v, R.id.tvDescription).setText(a.getDescription());

        // Tài liệu đính kèm (backend hiện chỉ có 1 tệp và chưa trả dung lượng)
        String url = a.getAttachmentUrl();
        boolean hasFile = url != null && !url.isEmpty();
        v.findViewById(R.id.cardAttachment).setVisibility(hasFile ? View.VISIBLE : View.GONE);
        if (hasFile) {
            String name = FileUtils.nameFromUrl(url);
            String label = FileUtils.typeLabel(name);
            boolean pdf = "PDF".equals(label);
            TextView tile = text(v, R.id.tvAttType);
            tile.setText(label);
            tile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(
                    pdf ? R.color.color_danger_container : R.color.color_primary_container)));
            tile.setTextColor(requireContext().getColor(pdf ? R.color.color_danger : R.color.color_info));
            text(v, R.id.tvAttName).setText(name);
            text(v, R.id.tvAttMeta).setText(label);
            View.OnClickListener open = x -> FileUtils.open(requireContext(), url);
            v.findViewById(R.id.rowAttachment).setOnClickListener(open);
            v.findViewById(R.id.btnDownload).setOnClickListener(open);
        }

        // Nút dưới cùng: chưa nộp -> Nộp bài, đã nộp -> Sửa bài nộp, đã chấm -> chỉ xem
        TextView button = text(v, R.id.btnAction);
        button.setText(a.isGraded() ? "Xem bài đã chấm"
                : a.isSubmitted() ? "Sửa bài nộp" : "Nộp bài");
        v.findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
        button.setOnClickListener(x -> getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AssignmentSubmitFragment.newInstance(assignmentId, false))
                .addToBackStack(null)
                .commit());
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}