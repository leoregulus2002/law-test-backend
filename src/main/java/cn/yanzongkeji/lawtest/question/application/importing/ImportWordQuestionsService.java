package cn.yanzongkeji.lawtest.question.application.importing;

import cn.yanzongkeji.lawtest.question.application.exception.QuestionValidationException;
import cn.yanzongkeji.lawtest.question.application.exception.WordFileValidationException;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.security.*;
import java.util.*;

@RequiredArgsConstructor
public class ImportWordQuestionsService implements ImportWordQuestionsUseCase {
    private final WordQuestionParser parser;
    private final QuestionBankRepository banks;
    private final QuestionRepository questions;

    @Override
    @Transactional
    public ImportResult importWord(String fileName, InputStream content, QuestionType questionType) throws IOException {
        validate(fileName, content, questionType);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<Question> parsed = parser.parse(new DigestInputStream(content, digest), questionType);
            ensureDistinctNumbers(parsed);
            String hash = HexFormat.of().formatHex(digest.digest());
            QuestionBank bank = QuestionBank.create(fileName, new QuestionBankCode(hash));
            QuestionBankRepository.ImportResult saved = banks.createIfAbsent(bank);
            if (saved.imported())
                questions.saveAll(saved.questionBankId(), parsed);
            return new ImportResult(saved.questionBankId().value(), hash, saved.imported(), parsed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private static void validate(String fileName, InputStream content, QuestionType questionType) {
        if (fileName == null || fileName.isBlank() || !fileName.toLowerCase(Locale.ROOT).endsWith(".docx"))
            throw new WordFileValidationException("仅支持 .docx 格式的 Word 文件");
        if (content == null)
            throw new WordFileValidationException("上传文件内容不能为空");
        if (questionType == null)
            throw new WordFileValidationException("题型不能为空");
    }

    private static void ensureDistinctNumbers(List<Question> questions) {
        Set<Integer> numbers = new HashSet<>();
        for (Question q : questions)
            if (!numbers.add(q.number().value()))
                throw new QuestionValidationException("同一题库内题号不能重复: " + q.number().value());
    }
}
