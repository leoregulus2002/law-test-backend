package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserPracticeQueryMapper {
    List<Long> findWrongQuestionIdsAfter(@Param("userId") long userId, @Param("cursor") Long cursor,
            @Param("limit") int limit);

    long countWrongQuestionIds(@Param("userId") long userId);

    Long findSequentialQuestionId(@Param("userId") long userId, @Param("scopeKey") String scopeKey);

    int upsertSequentialQuestionId(@Param("userId") long userId, @Param("scopeKey") String scopeKey,
            @Param("questionId") long questionId);
}
