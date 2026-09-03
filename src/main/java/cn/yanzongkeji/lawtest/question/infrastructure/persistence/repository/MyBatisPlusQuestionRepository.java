package cn.yanzongkeji.lawtest.question.infrastructure.persistence.repository;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.QuestionRepository;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.converter.QuestionPersistenceConverter;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.*;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class MyBatisPlusQuestionRepository implements QuestionRepository {
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final QuestionAnswerMapper answerMapper;

    /** 逐个保存导入后的题目，由应用服务事务保证整批写入的一致性。 */
    public void saveAll(QuestionBankId bankId, Collection<Question> items) {
        for (Question q : items)
            insert(bankId, q);
    }

    public QuestionId save(Question q) {
        if (q.id() == null)
            return insert(q.questionBankId(), q);
        clearChildren(q.id().value());
        QuestionDO d = QuestionPersistenceConverter.toQuestionDO(q, q.questionBankId());
        d.setId(q.id().value());
        questionMapper.updateContent(d);
        insertChildren(q.id().value(), q);
        return q.id();
    }

    private QuestionId insert(QuestionBankId bankId, Question q) {
        QuestionDO d = QuestionPersistenceConverter.toQuestionDO(q, bankId);
        questionMapper.insert(d);
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
            optionMapper.insert(d);
        }
        for (String label : q.answerKey().optionLabels()) {
            QuestionAnswerDO d = new QuestionAnswerDO();
            d.setQuestionId(id);
            d.setOptionLabel(label);
            answerMapper.insert(d);
        }
    }

    /** 查询主表和两个子表后恢复完整题目聚合。 */
    public Optional<Question> findById(QuestionId id) {
        QuestionDO question = questionMapper.selectById(id.value());
        if (question == null)
            return Optional.empty();
        return Optional.of(QuestionPersistenceConverter.toDomain(question,
                optionMapper.findByQuestionId(id.value()), answerMapper.findByQuestionId(id.value())));
    }

    public boolean deleteById(QuestionId id) {
        clearChildren(id.value());
        return questionMapper.deleteById(id.value()) > 0;
    }

    /** 按题库批量清理子表后删除题目主表，整个过程由应用服务事务包裹。 */
    public void deleteByBankId(QuestionBankId id) {
        answerMapper.deleteByBankId(id.value());
        optionMapper.deleteByBankId(id.value());
        questionMapper.deleteByBankId(id.value());
    }

    private void clearChildren(long id) {
        answerMapper.deleteByQuestionId(id);
        optionMapper.deleteByQuestionId(id);
    }
}
