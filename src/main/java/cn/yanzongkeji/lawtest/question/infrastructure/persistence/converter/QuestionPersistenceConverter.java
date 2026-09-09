package cn.yanzongkeji.lawtest.question.infrastructure.persistence.converter;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionAnswerDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionOptionDO;
import java.util.List;

/** Question 聚合与持久化数据对象之间的转换器。 */
public final class QuestionPersistenceConverter {

    private QuestionPersistenceConverter() {
    }

    /** 将 Question 聚合的主表数据转换为 QuestionDO。 */
    public static QuestionDO toQuestionDO(Question question, QuestionBankId questionBankId) {
        QuestionDO data = new QuestionDO();
        data.setQuestionBankId(questionBankId.value());
        data.setSequenceNo(question.number().value());
        data.setStem(question.stem());
        data.setAnalysis(question.analysis());
        data.setQuestionType(question.type().name());
        data.setStatus(question.status().name());
        return data;
    }

    /** 使用主表和全部子表数据恢复完整 Question 聚合。 */
    public static Question toDomain(QuestionDO question, List<QuestionOptionDO> options,
            List<QuestionAnswerDO> answers) {
        List<QuestionOption> domainOptions = options.stream()
                .map(option -> new QuestionOption(option.getLabel(), option.getContent()))
                .toList();
        List<String> answerLabels = answers.stream()
                .map(QuestionAnswerDO::getOptionLabel)
                .sorted()
                .toList();
        return Question.reconstitute(new QuestionId(question.getId()),
                new QuestionBankId(question.getQuestionBankId()), new QuestionNumber(question.getSequenceNo()),
                question.getStem(), domainOptions, new AnswerKey(answerLabels),
                QuestionType.valueOf(question.getQuestionType()), question.getAnalysis(),
                QuestionStatus.valueOf(question.getStatus()));
    }
}
