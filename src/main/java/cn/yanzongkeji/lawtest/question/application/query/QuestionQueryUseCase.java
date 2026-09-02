package cn.yanzongkeji.lawtest.question.application.query;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionPage;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionIdCursorPage;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import java.util.List;

public interface QuestionQueryUseCase {
    QuestionPage<QuestionBank> questionBanks(int page, int size);

    QuestionBank questionBank(long id);

    QuestionPage<Question> questions(long bankId, int page, int size);

    Question question(long id);

    /** 按游标查询全部或指定题库范围内的题目 ID。 */
    QuestionIdCursorPage questionIds(List<Long> questionBankIds, Long cursor, int size);

    /** 随机查询全部或指定题库范围内的一个题目 ID。 */
    long randomQuestionId(List<Long> questionBankIds);
}
