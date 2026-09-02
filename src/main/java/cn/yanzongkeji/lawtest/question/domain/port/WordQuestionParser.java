package cn.yanzongkeji.lawtest.question.domain.port;

import cn.yanzongkeji.lawtest.question.domain.model.Question;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Word 题目来源端口。
 */
public interface WordQuestionParser {

    List<Question> parse(InputStream content) throws IOException;
}
