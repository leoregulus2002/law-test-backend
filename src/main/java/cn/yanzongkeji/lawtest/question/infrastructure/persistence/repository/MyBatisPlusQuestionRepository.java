package cn.yanzongkeji.lawtest.question.infrastructure.persistence.repository;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.QuestionRepository;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.*;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class MyBatisPlusQuestionRepository implements QuestionRepository {
    private final QuestionMapper questions;
    private final QuestionOptionMapper options;
    private final QuestionAnswerMapper answers;

    public void saveAll(QuestionBankId bankId, Collection<Question> items) {
        for (Question q : items)
            insert(bankId, q);
    }

    public QuestionId save(Question q) {
        if (q.id() == null)
            return insert(q.questionBankId(), q);
        clearChildren(q.id().value());
        QuestionDO d = toDO(q, q.questionBankId());
        d.setId(q.id().value());
        questions.updateContent(d);
        insertChildren(q.id().value(), q);
        return q.id();
    }

    private QuestionId insert(QuestionBankId bankId, Question q) {
        QuestionDO d = toDO(q, bankId);
        questions.insert(d);
        insertChildren(d.getId(), q);
        return new QuestionId(d.getId());
    }

    private void insertChildren(long id, Question q) {
        short order = 1;
        for (QuestionOption o : q.options()) {
            QuestionOptionDO d = new QuestionOptionDO();
            d.setQuestionId(id);
            d.setLabel(o.label());
            d.setContent(o.content());
            d.setDisplayOrder(order++);
            options.insert(d);
        }
        for (String label : q.answerKey().optionLabels()) {
            QuestionAnswerDO d = new QuestionAnswerDO();
            d.setQuestionId(id);
            d.setOptionLabel(label);
            answers.insert(d);
        }
    }

    public Optional<Question> findById(QuestionId id) {
        QuestionDO d = questions.selectById(id.value());
        return d == null ? Optional.empty() : Optional.of(toDomain(d));
    }

    public List<Question> findPageByBankId(QuestionBankId bankId, int offset, int limit) {
        return questions.selectList(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getQuestionBankId, bankId.value()).orderByAsc(QuestionDO::getSequenceNo)
                .orderByAsc(QuestionDO::getId).last("limit " + limit + " offset " + offset)).stream()
                .map(this::toDomain).toList();
    }

    public long countByBankId(QuestionBankId id) {
        return questions
                .selectCount(new LambdaQueryWrapper<QuestionDO>().eq(QuestionDO::getQuestionBankId, id.value()));
    }

    /** 只读取游标之后的题目主键，并可按题库范围筛选。 */
    public List<QuestionId> findIdsAfter(List<QuestionBankId> questionBankIds, Long cursor, int limit) {
        LambdaQueryWrapper<QuestionDO> query = idQuery(questionBankIds).select(QuestionDO::getId)
                .orderByAsc(QuestionDO::getId).last("limit " + limit);
        if (cursor != null) {
            query.gt(QuestionDO::getId, cursor);
        }
        return questions.selectList(query).stream().map(QuestionDO::getId).map(QuestionId::new).toList();
    }

    /** 由 PostgreSQL 随机排序后只返回一个题目主键。 */
    public Optional<QuestionId> findRandomId(List<QuestionBankId> questionBankIds) {
        return questions.selectList(idQuery(questionBankIds).select(QuestionDO::getId).last("order by random() limit 1"))
                .stream().map(QuestionDO::getId).map(QuestionId::new).findFirst();
    }

    /** 为 ID 查询构造可选题库范围条件。 */
    private LambdaQueryWrapper<QuestionDO> idQuery(List<QuestionBankId> questionBankIds) {
        LambdaQueryWrapper<QuestionDO> query = new LambdaQueryWrapper<>();
        if (!questionBankIds.isEmpty()) {
            query.in(QuestionDO::getQuestionBankId, questionBankIds.stream().map(QuestionBankId::value).toList());
        }
        return query;
    }

    public boolean deleteById(QuestionId id) {
        clearChildren(id.value());
        return questions.deleteById(id.value()) > 0;
    }

    public void deleteByBankId(QuestionBankId id) {
        for (QuestionDO d : questions
                .selectList(new LambdaQueryWrapper<QuestionDO>().eq(QuestionDO::getQuestionBankId, id.value())))
            clearChildren(d.getId());
        questions.delete(new LambdaQueryWrapper<QuestionDO>().eq(QuestionDO::getQuestionBankId, id.value()));
    }

    private void clearChildren(long id) {
        answers.delete(new LambdaQueryWrapper<QuestionAnswerDO>().eq(QuestionAnswerDO::getQuestionId, id));
        options.delete(new LambdaQueryWrapper<QuestionOptionDO>().eq(QuestionOptionDO::getQuestionId, id));
    }

    private QuestionDO toDO(Question q, QuestionBankId bankId) {
        QuestionDO d = new QuestionDO();
        d.setQuestionBankId(bankId.value());
        d.setSequenceNo(q.number().value());
        d.setStem(q.stem());
        d.setAnalysis(q.analysis());
        d.setQuestionType(q.type().name());
        return d;
    }

    private Question toDomain(QuestionDO d) {
        List<QuestionOption> os = options
                .selectList(new LambdaQueryWrapper<QuestionOptionDO>().eq(QuestionOptionDO::getQuestionId, d.getId())
                        .orderByAsc(QuestionOptionDO::getDisplayOrder))
                .stream().map(o -> new QuestionOption(o.getLabel(), o.getContent())).toList();
        List<String> as = answers
                .selectList(new LambdaQueryWrapper<QuestionAnswerDO>().eq(QuestionAnswerDO::getQuestionId, d.getId()))
                .stream().map(QuestionAnswerDO::getOptionLabel).sorted().toList();
        return Question.reconstitute(new QuestionId(d.getId()), new QuestionBankId(d.getQuestionBankId()),
                new QuestionNumber(d.getSequenceNo()), d.getStem(), os, new AnswerKey(as),
                QuestionType.valueOf(d.getQuestionType()), d.getAnalysis());
    }
}
