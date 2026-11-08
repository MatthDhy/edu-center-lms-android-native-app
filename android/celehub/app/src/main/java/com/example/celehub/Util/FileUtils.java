package com.example.celehub.Util;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/** Chọn tệp từ điện thoại và gửi lên server (multipart), không đọc cả tệp vào bộ nhớ. */
public final class FileUtils {

    private FileUtils() { }

    /** Backend giới hạn 20 MB mỗi tệp. */
    public static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;

    /** Đuôi tệp backend nhận cho bài làm (UploadValidator.SUBMISSIONS). */
    public static final Set<String> SUBMISSION_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "png", "jpg", "jpeg", "webp", "heic"));

    /** Dùng để lọc trong trình chọn tệp của hệ thống. */
    public static final String[] SUBMISSION_MIME_TYPES = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/*"};

    /** Thông tin một tệp người dùng vừa chọn. size = -1 nếu không biết. */
    public static class Picked {
        public final Uri uri;
        public final String name;
        public final long size;
        public final String mime;

        Picked(Uri uri, String name, long size, String mime) {
            this.uri = uri;
            this.name = name;
            this.size = size;
            this.mime = mime;
        }
    }

    public static Picked query(ContentResolver cr, Uri uri) {
        String name = null;
        long size = -1;
        try (Cursor c = cr.query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int s = c.getColumnIndex(OpenableColumns.SIZE);
                if (n >= 0) name = c.getString(n);
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s);
            }
        } catch (RuntimeException ignored) {
            // Một số nguồn tệp không cho truy vấn, dùng giá trị mặc định bên dưới
        }
        if (name == null || name.isEmpty()) name = "tep";
        String mime = cr.getType(uri);
        if (mime == null) mime = "application/octet-stream";
        return new Picked(uri, name, size, mime);
    }

    /** "bai.PDF" -> "pdf"; không có đuôi thì trả chuỗi rỗng. */
    public static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Tên gửi lên server. Nếu tên tệp thiếu đuôi thì bổ sung từ loại tệp. */
    public static String uploadName(Picked p) {
        if (!extension(p.name).isEmpty()) return p.name;
        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(p.mime);
        return ext == null ? p.name : p.name + "." + ext;
    }

    /** "1.2 MB", "850 KB"; rỗng nếu không biết dung lượng. */
    public static String readableSize(long bytes) {
        if (bytes < 0) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return Math.round(bytes / 1024.0) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /** Nhãn ngắn hiện trên biểu tượng tệp. */
    public static String typeLabel(String name) {
        String ext = extension(name);
        switch (ext) {
            case "pdf": return "PDF";
            case "doc":
            case "docx": return "DOC";
            case "png":
            case "jpg":
            case "jpeg":
            case "webp":
            case "heic": return "IMG";
            default: return "FILE";
        }
    }

    /** Lấy tên tệp từ đường dẫn lưu trữ. */
    public static String nameFromUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        Uri u = Uri.parse(url);
        String name = u.getQueryParameter("name");      // đã tự giải mã %20, tiếng Việt...
        if (name != null && !name.isEmpty()) return name;
        String last = u.getLastPathSegment();
        return last == null ? "" : last;
    }

    /** Mở tệp bằng trình duyệt hoặc ứng dụng phù hợp. */
    public static void open(Context ctx, String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(ctx, "Không có tệp để mở", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, "Không có ứng dụng nào mở được tệp này", Toast.LENGTH_SHORT).show();
        }
    }

    /** Tạo phần multipart để gửi lên server. */
    public static MultipartBody.Part toPart(ContentResolver cr, Picked p, String partName) {
        RequestBody body = new UriBody(cr, p.uri, MediaType.parse(p.mime), p.size);
        return MultipartBody.Part.createFormData(partName, uploadName(p), body);
    }

    /** RequestBody đọc trực tiếp từ Uri khi gửi. */
    private static class UriBody extends RequestBody {
        private final ContentResolver cr;
        private final Uri uri;
        private final MediaType type;
        private final long size;

        UriBody(ContentResolver cr, Uri uri, MediaType type, long size) {
            this.cr = cr;
            this.uri = uri;
            this.type = type;
            this.size = size;
        }

        @Override
        public MediaType contentType() {
            return type;
        }

        @Override
        public long contentLength() {
            return size;   // -1 nếu không biết, OkHttp sẽ gửi dạng chunked
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            try (InputStream in = cr.openInputStream(uri)) {
                if (in == null) throw new IOException("Không đọc được tệp");
                try (Source source = Okio.source(in)) {
                    sink.writeAll(source);
                }
            }
        }
    }
    /** Đuôi tệp backend nhận cho tài liệu và đề bài (UploadValidator.DOCUMENTS). */
    public static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip",
            "png", "jpg", "jpeg", "webp"));

    /** Cho phép chọn mọi loại tệp, đuôi tệp sẽ được kiểm tra sau khi chọn. */
    public static final String[] ALL_MIME_TYPES = {"*/*"};

    /** Trường văn bản trong form multipart, mã hóa UTF-8 để giữ đúng tiếng Việt. */
    public static RequestBody text(String value) {
        return RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), value);
    }
}