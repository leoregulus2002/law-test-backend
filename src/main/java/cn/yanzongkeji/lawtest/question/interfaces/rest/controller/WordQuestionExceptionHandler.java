package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.application.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DuplicateKeyException;

@RestControllerAdvice
public class WordQuestionExceptionHandler {

    @ExceptionHandler({ WordFileValidationException.class, WordQuestionParseException.class,
            QuestionValidationException.class })
    ResponseEntity<ProblemDetail> handleInvalidWord(RuntimeException exception) {
        return ResponseEntity.badRequest().body(problemDetail(exception.getMessage()));
    }

    @ExceptionHandler({ QuestionNotFoundException.class, QuestionBankNotFoundException.class })
    ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ProblemDetail> handleConflict(DuplicateKeyException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "同一题库内题号不能重复");
        detail.setTitle("题目保存冲突");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> handleFileTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail("上传文件不能超过 10MB"));
    }

    private ProblemDetail problemDetail(String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Word 题目解析失败");
        return problemDetail;
    }
}
