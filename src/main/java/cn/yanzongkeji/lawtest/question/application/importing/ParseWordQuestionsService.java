package cn.yanzongkeji.lawtest.question.application.importing;

import cn.yanzongkeji.lawtest.question.application.exception.WordFileValidationException;

import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;
import cn.yanzongkeji.lawtest.question.domain.port.WordQuestionParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 解析用例。负责用例级输入规则，不理解 HTTP 或 Apache POI。
 */
public final class ParseWordQuestionsService implements ParseWordQuestionsUseCase {

    private final WordQuestionParser parser;

    public ParseWordQuestionsService(WordQuestionParser parser) {
        this.parser = Objects.requireNonNull(parser, "Word 解析器不能为空");
    }

    @Override
    public List<Question> parse(String fileName, InputStream content, QuestionType questionType) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new WordFileValidationException("请选择一个 .docx 文件");
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new WordFileValidationException("仅支持 .docx 格式的 Word 文件");
        }
        if (content == null) {
            throw new WordFileValidationException("上传文件内容不能为空");
        }
        if (questionType == null) {
            throw new WordFileValidationException("题型不能为空");
        }
        return parser.parse(content, questionType);
    }
}
