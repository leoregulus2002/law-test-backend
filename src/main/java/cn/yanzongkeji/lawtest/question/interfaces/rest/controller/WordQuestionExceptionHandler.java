package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.application.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class WordQuestionExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WordQuestionExceptionHandler.class);

    @ExceptionHandler({ WordFileValidationException.class, WordQuestionParseException.class,
            QuestionValidationException.class })
    ResponseEntity<ProblemDetail> handleInvalidWord(RuntimeException exception) {
        logClientError(HttpStatus.BAD_REQUEST, exception);
        return ResponseEntity.badRequest().body(problemDetail(exception.getMessage()));
    }

    @ExceptionHandler({ QuestionNotFoundException.class, QuestionBankNotFoundException.class })
    ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
        logClientError(HttpStatus.NOT_FOUND, exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ProblemDetail> handleConflict(DuplicateKeyException exception) {
        logClientError(HttpStatus.CONFLICT, exception);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "同一题库内题号不能重复");
        detail.setTitle("题目保存冲突");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> handleFileTooLarge(MaxUploadSizeExceededException exception) {
        logClientError(HttpStatus.BAD_REQUEST, exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail("上传文件不能超过 10MB"));
    }

    private ProblemDetail problemDetail(String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Word 题目解析失败");
        return problemDetail;
    }

    private void logClientError(HttpStatus status, RuntimeException exception) {
        LOG.warn("Question API request rejected: status={} exceptionType={}",
                status.value(), exception.getClass().getSimpleName());
    }
}
