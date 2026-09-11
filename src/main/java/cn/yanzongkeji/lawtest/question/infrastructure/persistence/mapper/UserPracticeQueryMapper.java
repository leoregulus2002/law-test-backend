package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import java.util.List;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserPracticeQueryMapper {
    List<Long> findWrongQuestionIdsAfter(@Param("userId") long userId, @Param("cursor") Long cursor,
            @Param("limit") int limit);

    long countWrongQuestionIds(@Param("userId") long userId);

    List<Long> findFavoriteQuestionIdsAfter(@Param("userId") long userId, @Param("cursor") Long cursor,
            @Param("limit") int limit);

    long countFavoriteQuestionIds(@Param("userId") long userId);

    boolean isFavoriteQuestion(@Param("userId") long userId, @Param("questionId") long questionId);

    int addFavoriteQuestion(@Param("userId") long userId, @Param("questionId") long questionId);

    int removeFavoriteQuestion(@Param("userId") long userId, @Param("questionId") long questionId);

    int recordDailyQuestion(@Param("userId") long userId, @Param("studyDate") LocalDate studyDate,
            @Param("questionId") long questionId);

    long countDailyQuestions(@Param("userId") long userId, @Param("studyDate") LocalDate studyDate);

    List<LocalDate> findStudyDates(@Param("userId") long userId);

    Long findSequentialQuestionId(@Param("userId") long userId, @Param("scopeKey") String scopeKey);

    int upsertSequentialQuestionId(@Param("userId") long userId, @Param("scopeKey") String scopeKey,
            @Param("questionId") long questionId);
}
