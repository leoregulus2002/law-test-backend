package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "分页响应")
public record PageResponse<T>(List<T> items, int page, int size, long total) {
}
