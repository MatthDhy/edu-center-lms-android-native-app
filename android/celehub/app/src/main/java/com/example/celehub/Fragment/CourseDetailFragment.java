package com.example.celehub.Fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Data.SessionManager;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.CourseItem;
import com.example.celehub.Model.MaterialItem;
import com.example.celehub.Model.StudentItem;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.HomeFormat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Chi tiết một lớp. Học viên xem tài liệu; giảng viên thấy thêm số liệu, nút thao tác và danh sách học viên. */
public class CourseDetailFragment extends Fragment {

    private static final String ARG_ID = "course_id";
    private static final String ARG_NAME = "course_name";

    private long courseId;
    private boolean isTeacher;

    public static CourseDetailFragment newInstance(long courseId, String courseName) {
        CourseDetailFragment f = new CourseDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, courseId);
        args.putString(ARG_NAME, courseName);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_detail, container, false);

        courseId = requireArguments().getLong(ARG_ID);
        String name = requireArguments().getString(ARG_NAME, "");
        isTeacher = "TEACHER".equals(SessionManager.get(requireContext()).getRole());

        ChildHeader.bind(this, view, isTeacher && !name.isEmpty() ? name : "Chi tiết môn học");
        text(view, R.id.tvDetailName).setText(name);

        if (isTeacher) {
            view.findViewById(R.id.llDetailStats).setVisibility(View.VISIBLE);
            view.findViewById(R.id.llTeacherActions).setVisibility(View.VISIBLE);
            view.findViewById(R.id.llStudentsSection).setVisibility(View.VISIBLE);


            view.findViewById(R.id.btnAddMaterial).setOnClickListener(v ->
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer,
                                    CreateMaterialFragment.newInstance(courseId, name))
                            .addToBackStack(null)
                            .commit());

            view.findViewById(R.id.btnAddAssignment).setOnClickListener(v ->
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer,
                                    CreateAssignmentFragment.newInstance(
                                            new long[]{courseId}, new String[]{name}, 0))
                            .addToBackStack(null)
                            .commit());
        }

        loadCourse();
        loadMaterials();
        if (isTeacher) loadStudents();
        return view;
    }

    // ===================== Gọi API =====================

    private void loadCourse() {
        ApiClient.get(requireContext()).getCourse(courseId)
                .enqueue(new Callback<ApiResponse<CourseItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<CourseItem>> call,
                                           Response<ApiResponse<CourseItem>> res) {
                        CourseItem c = dataOrToast(res, "Không tải được thông tin lớp");
                        if (c != null) bindCourse(c);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<CourseItem>> call, Throwable t) {
                        networkError();
                    }
                });
    }

    private void loadMaterials() {
        ApiClient.get(requireContext()).getMaterials(courseId)
                .enqueue(new Callback<ApiResponse<List<MaterialItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<MaterialItem>>> call,
                                           Response<ApiResponse<List<MaterialItem>>> res) {
                        List<MaterialItem> list = dataOrToast(res, "Không tải được tài liệu");
                        if (list != null) bindMaterials(list);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<MaterialItem>>> call, Throwable t) {
                        networkError();
                    }
                });
    }

    private void loadStudents() {
        ApiClient.get(requireContext()).getCourseStudents(courseId)
                .enqueue(new Callback<ApiResponse<List<StudentItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<StudentItem>>> call,
                                           Response<ApiResponse<List<StudentItem>>> res) {
                        List<StudentItem> list = dataOrToast(res, "Không tải được danh sách học viên");
                        if (list != null) bindStudents(list);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<StudentItem>>> call, Throwable t) {
                        networkError();
                    }
                });
    }

    /** Trả dữ liệu nếu hợp lệ; ngược lại báo lỗi và trả null. Cũng trả null nếu người dùng đã rời màn hình. */
    private <T> T dataOrToast(Response<ApiResponse<T>> res, String fallback) {
        if (!isAdded() || getView() == null) return null;
        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
            return res.body().getData();
        }
        Toast.makeText(requireContext(), ApiErrors.messageOf(res, fallback), Toast.LENGTH_SHORT).show();
        return null;
    }

    private void networkError() {
        if (!isAdded() || getView() == null) return;
        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
    }

    // ===================== Hiển thị =====================

    private void bindCourse(CourseItem c) {
        View view = getView();
        if (view == null) return;

        text(view, R.id.tvDetailName).setText(c.getCourseName());
        if (c.getTeacherName() != null && !c.getTeacherName().isEmpty()) {
            text(view, R.id.tvDetailTeacher).setText("Giáo viên: " + c.getTeacherName());
        }

        long total = c.getTotalSessions();
        String sessions = total > 0 ? c.getHeldCount() + "/" + total : String.valueOf(c.getHeldCount());
        text(view, R.id.tvDetailMeta).setText(isTeacher
                ? c.getStudentCount() + " học viên • " + sessions + " buổi"
                : sessions + " buổi đã học");

        if (isTeacher) {
            text(view, R.id.tvDetailStudents).setText(String.valueOf(c.getStudentCount()));
            text(view, R.id.tvDetailSessions).setText(sessions);
        }

        // Mô tả lớp hiện dưới tiêu đề "Tài liệu môn học" như Figma
        String desc = c.getDescription();
        TextView sub = text(view, R.id.tvMaterialsSub);
        if (desc != null && !desc.trim().isEmpty()) {
            sub.setText(desc);
            sub.setVisibility(View.VISIBLE);
        }
    }

    private void bindMaterials(List<MaterialItem> list) {
        View view = getView();
        if (view == null) return;
        LinearLayout container = view.findViewById(R.id.llMaterials);
        container.removeAllViews();
        view.findViewById(R.id.tvMaterialsEmpty).setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (MaterialItem m : list) {
            View row = inflater.inflate(R.layout.item_material, container, false);
            text(row, R.id.tvMaterialTitle).setText(m.getTitle());

            TextView desc = text(row, R.id.tvMaterialDesc);
            if (m.getDescription() == null || m.getDescription().trim().isEmpty()) {
                desc.setVisibility(View.GONE);
            } else {
                desc.setText(m.getDescription());
            }

            String by = m.getUploadedByName() == null ? "" : m.getUploadedByName();
            String date = HomeFormat.date(m.getCreatedAt());
            String meta = by.isEmpty() ? date : (date.isEmpty() ? by : by + " • " + date);
            TextView metaView = text(row, R.id.tvMaterialMeta);
            if (meta.isEmpty()) {
                metaView.setVisibility(View.GONE);
            } else {
                metaView.setText(meta);
            }
            if (isTeacher) {
                row.findViewById(R.id.ivChevron).setVisibility(View.GONE);
                View delete = row.findViewById(R.id.btnDeleteMaterial);
                delete.setVisibility(View.VISIBLE);
                delete.setOnClickListener(v -> confirmDeleteMaterial(m));
            }

            row.setOnClickListener(v -> openFile(m.getFileUrl()));
            container.addView(row);
        }
    }
    private void confirmDeleteMaterial(MaterialItem m) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa tài liệu?")
                .setMessage("Tài liệu \"" + m.getTitle() + "\" sẽ bị xóa khỏi lớp và học viên không còn thấy nữa.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteMaterial(m))
                .show();
    }
    private void deleteMaterial(MaterialItem m) {
        if (m.getId() == null) return;
        ApiClient.get(requireContext()).deleteMaterial(m.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> res) {
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đã xóa tài liệu", Toast.LENGTH_SHORT).show();
                            loadMaterials();   // tải lại danh sách
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Xóa tài liệu thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bindStudents(List<StudentItem> list) {
        View view = getView();
        if (view == null) return;
        LinearLayout container = view.findViewById(R.id.llStudents);
        container.removeAllViews();
        view.findViewById(R.id.tvStudentsEmpty).setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        text(view, R.id.tvStudentsTitle).setText("Học viên của lớp (" + list.size() + ")");

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (StudentItem s : list) {
            View row = inflater.inflate(R.layout.item_student, container, false);
            text(row, R.id.tvStudentAvatar).setText(initials(s.getFullName()));
            text(row, R.id.tvStudentName).setText(s.getFullName());

            StringBuilder contact = new StringBuilder();
            if (s.getEmail() != null && !s.getEmail().isEmpty()) contact.append(s.getEmail());
            if (s.getPhone() != null && !s.getPhone().isEmpty()) {
                if (contact.length() > 0) contact.append(" • ");
                contact.append(s.getPhone());
            }
            text(row, R.id.tvStudentContact).setText(contact.toString());
            container.addView(row);
        }
    }

    /** Mở tệp tài liệu bằng trình duyệt hoặc ứng dụng phù hợp. */
    private void openFile(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(requireContext(), "Tài liệu này chưa có tệp", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Không có ứng dụng nào mở được tệp này",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** "Gia Huy" -> "GH" (chữ cái đầu của từ đầu và từ cuối). */
    private static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}