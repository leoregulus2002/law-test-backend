package cn.yanzongkeji.lawtest.question.application.importing;

import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ParseWordQuestionsUseCase {

    List<Question> parse(String fileName, InputStream content, QuestionType questionType) throws IOException;
}
