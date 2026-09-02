package cn.yanzongkeji.lawtest.question.application.query;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionPage;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionIdCursorPage;
import cn.yanzongkeji.lawtest.question.application.exception.*;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class QuestionQueryService implements QuestionQueryUseCase {
    private final QuestionBankRepository banks;
    private final QuestionRepository questions;

    public QuestionPage<QuestionBank> questionBanks(int page, int size) {
        int[] p = page(page, size);
        return new QuestionPage<>(banks.findPage(p[0], p[1]), page, p[1], banks.count());
    }

    public QuestionBank questionBank(long id) {
        return banks.findById(new QuestionBankId(id)).orElseThrow(() -> new QuestionBankNotFoundException(id));
    }

    public QuestionPage<Question> questions(long bankId, int page, int size) {
        QuestionBankId id = new QuestionBankId(bankId);
        if (banks.findById(id).isEmpty())
            throw new QuestionBankNotFoundException(bankId);
        int[] p = page(page, size);
        return new QuestionPage<>(questions.findPageByBankId(id, p[0], p[1]), page, p[1], questions.countByBankId(id));
    }

    public Question question(long id) {
        return questions.findById(new QuestionId(id)).orElseThrow(() -> new QuestionNotFoundException(id));
    }

    /** 按 ID 游标查询全部或指定题库范围内的题目 ID。 */
    public QuestionIdCursorPage questionIds(List<Long> questionBankIds, Long cursor, int size) {
        validateCursor(cursor, size);
        List<Long> ids = questions.findIdsAfter(toBankIds(questionBankIds), cursor, size + 1).stream()
                .map(QuestionId::value).toList();
        boolean hasNext = ids.size() > size;
        List<Long> items = hasNext ? ids.subList(0, size) : ids;
        Long nextCursor = hasNext ? items.getLast() : null;
        return new QuestionIdCursorPage(items, nextCursor, hasNext);
    }

    /** 在全部或指定题库范围内随机获取一个题目 ID。 */
    public long randomQuestionId(List<Long> questionBankIds) {
        return questions.findRandomId(toBankIds(questionBankIds)).orElseThrow(QuestionNotFoundException::new).value();
    }

    /** 将可选的 HTTP 题库 ID 参数转换为去重后的领域标识。 */
    private static List<QuestionBankId> toBankIds(List<Long> questionBankIds) {
        if (questionBankIds == null || questionBankIds.isEmpty()) {
            return List.of();
        }
        return questionBankIds.stream().distinct().map(QuestionBankId::new).toList();
    }

    /** 校验游标和每页条数，确保查询边界稳定且受限。 */
    private static void validateCursor(Long cursor, int size) {
        if (cursor != null && cursor <= 0) {
            throw new QuestionValidationException("cursor 必须为正整数");
        }
        if (size < 1 || size > 100) {
            throw new QuestionValidationException("size 必须在 1 到 100 之间");
        }
    }

    private static int[] page(int page, int size) {
        if (page < 0)
            throw new QuestionValidationException("page 不能小于 0");
        int actual = size == 0 ? 20 : size;
        if (actual < 1 || actual > 100)
            throw new QuestionValidationException("size 必须在 1 到 100 之间");
        return new int[] { Math.multiplyExact(page, actual), actual };
    }
}
