package com.example.celehub.API;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.celehub.Data.SessionManager;

/** Tạo Retrofit một lần duy nhất cho cả app. Dùng: ApiClient.get(context).login(...) */
public final class ApiClient {

    /**
     * Địa chỉ backend.
     *  - Máy ảo Android: 10.0.2.2 chính là "localhost" của máy tính đang chạy backend.
     *  - Điện thoại thật: đổi thành IP LAN của máy tính, ví dụ "http://192.168.1.10:8080/"
     *    (cùng mạng Wi-Fi) và sửa luôn domain trong res/xml/network_security_config.xml.
     */
    public static final String BASE_URL = "http://10.0.2.2:8080/";

    private static ApiService service;

    private ApiClient() { }

    public static synchronized ApiService get(Context context) {
        if (service == null) {
            Context appContext = context.getApplicationContext();

            // BASIC chỉ ghi dòng "POST /api/auth/login 200", KHÔNG ghi nội dung (tránh lộ mật khẩu vào log)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(appContext, SessionManager.get(appContext)))
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(ApiService.class);
        }
        return service;
    }
}