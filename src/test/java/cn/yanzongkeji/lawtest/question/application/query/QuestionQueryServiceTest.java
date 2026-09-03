package cn.yanzongkeji.lawtest.question.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionIdCursorPage;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QuestionQueryServiceTest {

    @Test
    void reportsTheTotalNumberOfQuestionsAlongsideCursorPageItems() throws Exception {
        QuestionQueryService service = new QuestionQueryService(null, null, null, new ThreeQuestionPracticeQuery());

        QuestionIdCursorPage page = service.questionIds(List.of(), null, 2);

        assertThat(List.of(page.getClass().getRecordComponents()).stream().map(RecordComponent::getName))
                .contains("total");
        long total = (long) page.getClass().getMethod("total").invoke(page);
        assertThat(total).isEqualTo(3L);
    }

    private static final class ThreeQuestionPracticeQuery implements QuestionPracticeQuery {
        @Override
        public long countByBankIds(List<QuestionBankId> questionBankIds) {
            return 3;
        }

        @Override
        public List<QuestionId> findIdsAfter(List<QuestionBankId> questionBankIds, Long cursor, int limit) {
            return List.of(new QuestionId(101), new QuestionId(102), new QuestionId(103));
        }

        @Override public Optional<QuestionId> findRandomId(List<QuestionBankId> questionBankIds) { throw unsupported(); }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("This test only exercises cursor ID queries");
        }
    }
}
