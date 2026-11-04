package com.celehub.backend.quiz;

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
import com.celehub.backend.quiz.QuizDtos.QuizOptionInput;
import com.celehub.backend.quiz.QuizDtos.QuizQuestionInput;
import com.celehub.backend.storage.UploadValidator;

/** Đọc / tạo file Excel mẫu: question_text, option_a..d, correct_answer (A-D), explanation. Công thức viết LaTeX $...$. */
@Service
public class QuizExcelService {
    private static final String[] HEADER = {"question_text", "option_a", "option_b", "option_c", "option_d",
            "correct_answer", "explanation"};
    private static final int MAX_QUESTIONS = 200;
    private static final int MAX_ERRORS = 10;

    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("quiz");
            Row h = sh.createRow(0);
            for (int i = 0; i < HEADER.length; i++) h.createCell(i).setCellValue(HEADER[i]);
            Row r = sh.createRow(1);
            String[] ex = {"Nghiệm của phương trình $x^2 - 4x + 3 = 0$ là", "$x = 1$ hoặc $x = 3$", "$x = -1$ hoặc $x = -3$",
                    "$x = 2$", "Vô nghiệm", "A", "Phân tích $(x-1)(x-3)=0$"};
            for (int i = 0; i < ex.length; i++) r.createCell(i).setCellValue(ex[i]);
            for (int i = 0; i < HEADER.length; i++) sh.setColumnWidth(i, i == 0 || i == 6 ? 12000 : 6000);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được file mẫu");
        }
    }

    public List<QuizQuestionInput> parse(MultipartFile file) {
        UploadValidator.require(file, UploadValidator.EXCEL, 5L * 1024 * 1024, "Excel (.xlsx)");
        List<String> errors = new ArrayList<>();
        List<QuizQuestionInput> result = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new AppException(HttpStatus.BAD_REQUEST, "File Excel trống");
            Map<String, Integer> col = new HashMap<>();
            for (Cell c : header) col.put(fmt.formatCellValue(c).trim().toLowerCase(Locale.ROOT), c.getColumnIndex());
            for (String need : HEADER) {
                if (!col.containsKey(need)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File thiếu cột \"" + need + "\". Hãy dùng file mẫu.");
                }
            }
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String text = cell(row, col, "question_text", fmt);
                String[] opt = {cell(row, col, "option_a", fmt), cell(row, col, "option_b", fmt),
                        cell(row, col, "option_c", fmt), cell(row, col, "option_d", fmt)};
                String ans = cell(row, col, "correct_answer", fmt).toUpperCase(Locale.ROOT);
                String exp = cell(row, col, "explanation", fmt);
                if (text.isEmpty() && ans.isEmpty() && opt[0].isEmpty() && opt[1].isEmpty()
                        && opt[2].isEmpty() && opt[3].isEmpty()) continue;   // dòng trống

                int line = r + 1;
                String err = null;
                if (result.size() + errors.size() >= MAX_QUESTIONS) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Tối đa " + MAX_QUESTIONS + " câu mỗi đề");
                }
                if (text.isEmpty()) err = "thiếu nội dung câu hỏi";
                else if (opt[0].isEmpty() || opt[1].isEmpty()) err = "phải có ít nhất đáp án A và B";
                else if (opt[2].isEmpty() && !opt[3].isEmpty()) err = "có đáp án D nhưng thiếu đáp án C";
                else if (!ans.matches("[A-D]")) err = "correct_answer phải là A, B, C hoặc D";
                else if (opt[ans.charAt(0) - 'A'].isEmpty()) err = "đáp án đúng " + ans + " đang để trống";
                else for (String o : opt) if (o.length() > 500) { err = "đáp án dài quá 500 ký tự"; break; }

                if (err != null) {
                    if (errors.size() < MAX_ERRORS) errors.add("Dòng " + line + ": " + err);
                    else if (errors.size() == MAX_ERRORS) errors.add("...");
                    continue;
                }
                List<QuizOptionInput> opts = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    if (!opt[i].isEmpty()) opts.add(new QuizOptionInput(opt[i], i == ans.charAt(0) - 'A'));
                }
                result.add(new QuizQuestionInput(text, exp.isEmpty() ? null : exp, opts));
            }
        } catch (AppException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không đọc được file Excel (.xlsx)");
        }
        if (!errors.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File có lỗi — " + String.join("; ", errors));
        }
        if (result.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "File không có câu hỏi nào");
        return result;
    }

    private static String cell(Row row, Map<String, Integer> col, String name, DataFormatter fmt) {
        Cell c = row.getCell(col.get(name));
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }
}
