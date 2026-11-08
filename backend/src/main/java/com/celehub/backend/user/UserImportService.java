package com.celehub.backend.user;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.AppException;
import com.celehub.backend.storage.UploadValidator;
import com.celehub.backend.user.dto.CreateUserRequest;

/** Import học viên / giảng viên từ Excel: cột fullName, email, phone, role (STUDENT mặc định). Mỗi dòng 1 giao dịch riêng. */
@Service
public class UserImportService {
    public record RowError(int row, String email, String error) {}
    public record ImportResult(int created, List<RowError> failed) {}

    private static final int MAX_ROWS = 500;
    private static final String[] HEADER = {"fullName", "email", "phone", "role"};

    private final UserService userService;

    public UserImportService(UserService userService) {
        this.userService = userService;
    }

    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("users");
            Row h = sh.createRow(0);
            for (int i = 0; i < HEADER.length; i++) h.createCell(i).setCellValue(HEADER[i]);
            String[][] ex = {{"Nguyễn Văn An", "an.nguyen@example.com", "0912345678", "STUDENT"},
                    {"Trần Thị Bình", "binh.tran@example.com", "", "TEACHER"}};
            for (int r = 0; r < ex.length; r++) {
                Row row = sh.createRow(r + 1);
                for (int c = 0; c < 4; c++) row.createCell(c).setCellValue(ex[r][c]);
            }
            for (int i = 0; i < 4; i++) sh.setColumnWidth(i, 7000);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được file mẫu");
        }
    }

    public ImportResult importFile(MultipartFile file) {
        UploadValidator.require(file, UploadValidator.EXCEL, 5L * 1024 * 1024, "Excel (.xlsx)");
        List<RowError> failed = new ArrayList<>();
        int created = 0;
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new AppException(HttpStatus.BAD_REQUEST, "File Excel trống");
            Map<String, Integer> col = new HashMap<>();
            for (Cell c : header) col.put(fmt.formatCellValue(c).trim().toLowerCase(Locale.ROOT), c.getColumnIndex());
            for (String need : new String[]{"fullname", "email"}) {
                if (!col.containsKey(need)) throw new AppException(HttpStatus.BAD_REQUEST, "File thiếu cột \"" + need + "\". Hãy dùng file mẫu.");
            }
            int rows = 0;
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String name = cell(row, col, "fullname", fmt);
                String email = cell(row, col, "email", fmt);
                String phone = cell(row, col, "phone", fmt).replace(" ", "");
                String roleText = cell(row, col, "role", fmt).toUpperCase(Locale.ROOT);
                if (name.isEmpty() && email.isEmpty()) continue;
                if (++rows > MAX_ROWS) throw new AppException(HttpStatus.BAD_REQUEST, "Tối đa " + MAX_ROWS + " dòng mỗi lần nhập");
                try {
                    Role role = roleText.isEmpty() ? Role.STUDENT : Role.valueOf(roleText);
                    if (name.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu họ tên");
                    if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new AppException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
                    if (!phone.isEmpty() && !phone.matches("^(0|\\+84)[0-9]{9}$")) throw new AppException(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ");
                    userService.createUser(new CreateUserRequest(name, email, phone, role));
                    created++;
                } catch (IllegalArgumentException e) {
                    failed.add(new RowError(r + 1, email, "role chỉ nhận STUDENT hoặc TEACHER"));
                } catch (AppException e) {
                    failed.add(new RowError(r + 1, email, e.getMessage()));
                }
            }
        } catch (AppException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không đọc được file Excel (.xlsx)");
        }
        return new ImportResult(created, failed);
    }

    private static String cell(Row row, Map<String, Integer> col, String name, DataFormatter fmt) {
        Integer i = col.get(name);
        if (i == null) return "";
        Cell c = row.getCell(i);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }
}
