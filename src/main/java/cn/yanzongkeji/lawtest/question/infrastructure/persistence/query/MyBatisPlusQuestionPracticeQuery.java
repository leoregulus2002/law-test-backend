package cn.yanzongkeji.lawtest.question.infrastructure.persistence.query;

import cn.yanzongkeji.lawtest.question.application.query.QuestionPracticeQuery;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.QuestionQueryMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis-Plus 的练习场景题目查询实现。 */
@Repository
@RequiredArgsConstructor
public class MyBatisPlusQuestionPracticeQuery implements QuestionPracticeQuery {
    private final QuestionQueryMapper queryMapper;

    @Override
    public long countByBankIds(List<QuestionBankId> questionBankIds) {
        return queryMapper.countByBankIds(toValues(questionBankIds));
    }

    @Override
    public List<QuestionId> findIdsAfter(List<QuestionBankId> questionBankIds, Long cursor, int limit) {
        return queryMapper.findIdsAfter(toValues(questionBankIds), cursor, limit).stream()
                .map(QuestionId::new)
                .toList();
    }

    @Override
    public Optional<QuestionId> findRandomId(List<QuestionBankId> questionBankIds) {
        return Optional.ofNullable(queryMapper.findRandomId(toValues(questionBankIds))).map(QuestionId::new);
    }

    /** 将领域标识转换为查询 Mapper 所需的数据库主键值。 */
    private static List<Long> toValues(List<QuestionBankId> questionBankIds) {
        return questionBankIds.stream().map(QuestionBankId::value).toList();
    }
}
