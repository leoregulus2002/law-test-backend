package cn.yanzongkeji.lawtest.question.application.dto;

import java.util.List;

public record QuestionPage<T>(List<T> items, int page, int size, long total) {
}
