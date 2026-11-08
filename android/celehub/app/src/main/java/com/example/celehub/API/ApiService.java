package com.example.celehub.API;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.AuthResponse;
import com.example.celehub.Model.CourseItem;
import com.example.celehub.Model.GradeBody;
import com.example.celehub.Model.HomeStudentResponse;
import com.example.celehub.Model.HomeTeacherResponse;
import com.example.celehub.Model.LoginRequest;
import com.example.celehub.Model.ForceChangePasswordRequest;
import com.example.celehub.Model.ChangePasswordRequest;
import com.example.celehub.Model.MaterialItem;
import com.example.celehub.Model.StudentItem;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.Model.SubmissionsOverview;
import com.example.celehub.Model.UpdateAssignmentBody;
import com.example.celehub.Model.UserDto;
import com.example.celehub.Model.ForgotPasswordRequest;
import com.example.celehub.Model.VerifyOtpRequest;
import com.example.celehub.Model.VerifyOtpResponse;
import com.example.celehub.Model.ResetPasswordRequest;
import com.example.celehub.Model.UpdateProfileRequest;

import java.util.List;
import java.util.Map;

/** Danh sách các API của backend mà app gọi. Mỗi hàm = một endpoint. */
public interface ApiService {

    @POST("api/auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("api/auth/force-change-password")
    Call<ApiResponse<Void>> forceChangePassword(@Body ForceChangePasswordRequest request);

    @POST("api/auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest request);

    @GET("api/users/me")
    Call<ApiResponse<UserDto>> getMe();

    @GET("api/home/student")
    Call<ApiResponse<HomeStudentResponse>> getHomeStudent();

    @GET("api/home/teacher")
    Call<ApiResponse<HomeTeacherResponse>> getHomeTeacher();
    @POST("api/auth/logout")
    Call<ApiResponse<Void>> logout();

    @POST("api/auth/forgot-password/request")
    Call<ApiResponse<Void>> forgotRequest(@Body ForgotPasswordRequest request);

    @POST("api/auth/forgot-password/verify")
    Call<ApiResponse<VerifyOtpResponse>> forgotVerify(@Body VerifyOtpRequest request);

    @POST("api/auth/forgot-password/reset")
    Call<ApiResponse<Void>> forgotReset(@Body ResetPasswordRequest request);

    @PATCH("api/users/me")
    Call<ApiResponse<UserDto>> updateProfile(@Body UpdateProfileRequest request);
    /** Học viên: lớp đang học. Giảng viên: lớp đang phụ trách. */
    @GET("api/courses/mine")
    Call<ApiResponse<List<CourseItem>>> getMyCourses();
    @GET("api/courses/{id}")
    Call<ApiResponse<CourseItem>> getCourse(@Path("id") long id);

    @GET("api/courses/{id}/materials")
    Call<ApiResponse<List<MaterialItem>>> getMaterials(@Path("id") long courseId);

    /** Chỉ giảng viên (và admin). */
    @GET("api/courses/{id}/students")
    Call<ApiResponse<List<StudentItem>>> getCourseStudents(@Path("id") long courseId);
    /** Học viên: bài tập của mọi lớp đang học, kèm trạng thái. */
    @GET("api/assignments/mine")
    Call<ApiResponse<List<AssignmentItem>>> getMyAssignments();

    @GET("api/assignments/{id}")
    Call<ApiResponse<AssignmentItem>> getAssignment(@Path("id") long id);

    /** Nộp lần đầu hoặc nộp lại (khi chưa được chấm). Chỉ nhận một phần "file". */
    @Multipart
    @POST("api/assignments/{id}/submit")
    Call<ApiResponse<SubmissionItem>> submitAssignment(@Path("id") long id,
                                                       @Part MultipartBody.Part file);
    /** Bài tập của một lớp. Giảng viên nhận thêm số bài đã nộp và đã chấm. */
    @GET("api/courses/{courseId}/assignments")
    Call<ApiResponse<List<AssignmentItem>>> getCourseAssignments(@Path("courseId") long courseId);

    /** Giao bài tập mới. Chỉ title và deadline bắt buộc; description và file có thể là null. */
    @Multipart
    @POST("api/courses/{courseId}/assignments")
    Call<ApiResponse<AssignmentItem>> createAssignment(@Path("courseId") long courseId,
                                                       @Part("title") RequestBody title,
                                                       @Part("description") RequestBody description,
                                                       @Part("deadline") RequestBody deadline,
                                                       @Part MultipartBody.Part file);
    /** Giảng viên: bài nộp của một bài tập (kèm thông tin bài). */
    @GET("api/assignments/{id}/submissions")
    Call<ApiResponse<SubmissionsOverview>> getSubmissions(@Path("id") long assignmentId);

    /** Chấm điểm. Backend từ chối điểm âm hoặc vượt điểm tối đa, và gửi thông báo cho học viên. */
    @PATCH("api/submissions/{id}/grade")
    Call<ApiResponse<SubmissionItem>> gradeSubmission(@Path("id") long submissionId,
                                                      @Body GradeBody body);
    /** Giảng viên đăng tài liệu cho lớp. title và file bắt buộc, description tùy chọn. */
    @Multipart
    @POST("api/courses/{courseId}/materials")
    Call<ApiResponse<MaterialItem>> uploadMaterial(@Path("courseId") long courseId,
                                                   @Part("title") RequestBody title,
                                                   @Part("description") RequestBody description,
                                                   @Part MultipartBody.Part file);
    /** Giảng viên xóa tài liệu của lớp mình (xóa cả tệp, không khôi phục được). */
    @DELETE("api/materials/{id}")
    Call<ApiResponse<Void>> deleteMaterial(@Path("id") long materialId);

    @DELETE("api/assignments/{id}/submission")
    Call<ApiResponse<Void>> deleteMySubmission(@Path("id") long assignmentId);
    /** Giảng viên sửa bài tập. Đổi hạn nộp thì backend tự gửi thông báo cho học viên. */
    @PUT("api/assignments/{id}")
    Call<ApiResponse<AssignmentItem>> updateAssignment(@Path("id") long id, @Body UpdateAssignmentBody body);

    /** Giảng viên xóa bài tập, kèm toàn bộ bài nộp và tệp (không khôi phục được). */
    @DELETE("api/assignments/{id}")
    Call<ApiResponse<Void>> deleteAssignment(@Path("id") long id);

    /** Nhắc các học viên chưa nộp. Trả về {"reminded": số người}. */
    @POST("api/assignments/{id}/remind-all")
    Call<ApiResponse<Map<String, Integer>>> remindAll(@Path("id") long id);
    /** Giảng viên thay tệp đề bài (hoặc thêm nếu bài chưa có). Backend tự xóa tệp cũ. */
    @Multipart
    @POST("api/assignments/{id}/attachment")
    Call<ApiResponse<AssignmentItem>> replaceAssignmentAttachment(@Path("id") long id,
                                                                  @Part MultipartBody.Part file);

    /** Giảng viên xóa tệp đề bài. */
    @DELETE("api/assignments/{id}/attachment")
    Call<ApiResponse<AssignmentItem>> removeAssignmentAttachment(@Path("id") long id);
}