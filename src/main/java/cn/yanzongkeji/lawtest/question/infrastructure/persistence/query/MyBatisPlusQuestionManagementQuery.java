package cn.yanzongkeji.lawtest.question.infrastructure.persistence.query;

import cn.yanzongkeji.lawtest.question.application.query.QuestionManagementQuery;
import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionListItem;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionListFilter;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.converter.QuestionPersistenceConverter;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionAnswerDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionOptionDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.QuestionQueryMapper;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis-Plus 的题目管理查询实现。 */
@Repository
@RequiredArgsConstructor
public class MyBatisPlusQuestionManagementQuery implements QuestionManagementQuery {
    private final QuestionQueryMapper queryMapper;
    private final QuestionMapper questionMapper;

    /**
     * 使用一次主表分页查询和两次子表批量查询恢复整页聚合，避免逐题查询子表。
     */
    @Override
    public List<Question> findPageByBankId(QuestionBankId bankId, int offset, int limit) {
        List<QuestionDO> questions = questionMapper.selectList(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getQuestionBankId, bankId.value()).orderByAsc(QuestionDO::getSequenceNo)
                .orderByAsc(QuestionDO::getId).last("limit " + limit + " offset " + offset));
        if (questions.isEmpty())
            return List.of();

        List<Long> questionIds = questions.stream().map(QuestionDO::getId).toList();
        Map<Long, List<QuestionOptionDO>> optionsByQuestionId = queryMapper.findOptionsByQuestionIds(questionIds)
                .stream().collect(Collectors.groupingBy(QuestionOptionDO::getQuestionId));
        Map<Long, List<QuestionAnswerDO>> answersByQuestionId = queryMapper.findAnswersByQuestionIds(questionIds)
                .stream().collect(Collectors.groupingBy(QuestionAnswerDO::getQuestionId));

        return questions.stream()
                .map(question -> QuestionPersistenceConverter.toDomain(question,
                        optionsByQuestionId.getOrDefault(question.getId(), List.of()),
                        answersByQuestionId.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    @Override
    public long countByBankId(QuestionBankId bankId) {
        return questionMapper.selectCount(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getQuestionBankId, bankId.value()));
    }

    @Override
    public List<QuestionListItem> findPage(int offset, int limit, QuestionListFilter filter) {
        return queryMapper.findPage(offset, limit, filter).stream()
                .map(item -> new QuestionListItem(item.getId(), item.getQuestionBankId(), item.getQuestionBankName(),
                        item.getSequenceNo(), item.getStem(), QuestionType.valueOf(item.getQuestionType()),
                        QuestionStatus.valueOf(item.getStatus())))
                .toList();
    }

    @Override
    public long count(QuestionListFilter filter) {
        return queryMapper.count(filter);
    }
}
