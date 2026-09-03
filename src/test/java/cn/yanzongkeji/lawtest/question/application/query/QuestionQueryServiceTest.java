package cn.yanzongkeji.lawtest.question.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionIdCursorPage;
import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import cn.yanzongkeji.lawtest.question.domain.port.QuestionRepository;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QuestionQueryServiceTest {

    @Test
    void reportsTheTotalNumberOfQuestionsAlongsideCursorPageItems() throws Exception {
        QuestionQueryService service = new QuestionQueryService(null, new ThreeQuestionRepository());

        QuestionIdCursorPage page = service.questionIds(List.of(), null, 2);

        assertThat(List.of(page.getClass().getRecordComponents()).stream().map(RecordComponent::getName))
                .contains("total");
        long total = (long) page.getClass().getMethod("total").invoke(page);
        assertThat(total).isEqualTo(3L);
    }

    private static final class ThreeQuestionRepository implements QuestionRepository {
        @Override
        public List<QuestionId> findIdsAfter(List<QuestionBankId> questionBankIds, Long cursor, int limit) {
            return List.of(new QuestionId(101), new QuestionId(102), new QuestionId(103));
        }

        @Override public void saveAll(QuestionBankId bankId, Collection<Question> questions) { throw unsupported(); }
        @Override public QuestionId save(Question question) { throw unsupported(); }
        @Override public Optional<Question> findById(QuestionId questionId) { throw unsupported(); }
        @Override public List<Question> findPageByBankId(QuestionBankId bankId, int offset, int limit) { throw unsupported(); }
        @Override public long countByBankId(QuestionBankId id) { throw unsupported(); }
        @Override public Optional<QuestionId> findRandomId(List<QuestionBankId> questionBankIds) { throw unsupported(); }
        @Override public boolean deleteById(QuestionId questionId) { throw unsupported(); }
        @Override public void deleteByBankId(QuestionBankId id) { throw unsupported(); }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("This test only exercises cursor ID queries");
        }
    }
}
