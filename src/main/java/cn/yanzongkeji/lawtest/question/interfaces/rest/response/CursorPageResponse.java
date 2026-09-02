package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 通用游标分页响应。 */
@Schema(description = "游标分页响应")
public record CursorPageResponse<T>(List<T> items, Long nextCursor, boolean hasNext) {
}
