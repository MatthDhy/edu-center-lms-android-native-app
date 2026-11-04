package com.celehub.backend.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mỗi phút: tự nộp & chấm các bài quiz đã quá giờ (kể cả khi học viên không mở app). Mỗi bài 1 transaction riêng. */
@Component
public class QuizAutoSubmitJob {
    private static final Logger log = LoggerFactory.getLogger(QuizAutoSubmitJob.class);
    private final QuizService quizService;

    public QuizAutoSubmitJob(QuizService quizService) {
        this.quizService = quizService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        int n = 0;
        for (Long id : quizService.findExpiredAttemptIds()) {
            try {
                quizService.autoSubmitOne(id);
                n++;
            } catch (Exception e) {
                log.warn("Tự nộp quiz attempt {} lỗi: {}", id, e.getMessage());
            }
        }
        if (n > 0) log.info("Đã tự nộp {} bài quiz hết giờ", n);
    }
}
