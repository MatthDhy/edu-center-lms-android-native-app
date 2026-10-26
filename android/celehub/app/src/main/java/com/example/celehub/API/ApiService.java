package com.example.celehub.API;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AuthResponse;
import com.example.celehub.Model.LoginRequest;
import com.example.celehub.Model.ForceChangePasswordRequest;
import com.example.celehub.Model.ChangePasswordRequest;
import com.example.celehub.Model.UserDto;

/** Danh sách các API của backend mà app gọi. Mỗi hàm = một endpoint. */
public interface ApiService {

    @POST("api/auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("api/auth/force-change-password")
    Call<ApiResponse<Void>> forceChangePassword(@Body ForceChangePasswordRequest request);

    @POST("api/auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest request);

    @POST("api/auth/forgot-password/request") Call<ApiResponse<Void>> forgotRequest(@Body ForgotPasswordRequest r);

    @POST("api/auth/forgot-password/verify")  Call<ApiResponse<VerifyOtpResponse>> forgotVerify(@Body VerifyOtpRequest r);
    
    @POST("api/auth/forgot-password/reset")   Call<ApiResponse<Void>> forgotReset(@Body ResetPasswordRequest r);

    @GET("api/users/me")
    Call<ApiResponse<UserDto>> getMe();
}