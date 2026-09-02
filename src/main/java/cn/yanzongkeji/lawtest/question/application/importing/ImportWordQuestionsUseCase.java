package cn.yanzongkeji.lawtest.question.application.importing;

import cn.yanzongkeji.lawtest.question.domain.model.Question;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ImportWordQuestionsUseCase {
    ImportResult importWord(String fileName, InputStream content) throws IOException;

    record ImportResult(long questionBankId, String questionBankCode, boolean imported, List<Question> questions) {
    }
}
