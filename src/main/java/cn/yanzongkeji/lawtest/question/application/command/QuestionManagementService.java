package cn.yanzongkeji.lawtest.question.application.command;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionCommand;
import cn.yanzongkeji.lawtest.question.application.exception.*;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RequiredArgsConstructor
public class QuestionManagementService implements QuestionManagementUseCase {
    private final QuestionBankRepository banks;
    private final QuestionRepository questions;

    @Override
    @Transactional
    public Question create(long bankId, QuestionCommand command) {
        QuestionBankId id = new QuestionBankId(bankId);
        if (banks.findById(id).isEmpty())
            throw new QuestionBankNotFoundException(bankId);
        Question question = toQuestion(id, command);
        QuestionId questionId = questions.save(question);
        return Question.reconstitute(questionId, id, question.number(), question.stem(), question.options(),
                question.answerKey(), question.type(), question.analysis());
    }

    @Override
    @Transactional
    public Question replace(long questionId, QuestionCommand command) {
        Question existing = questions.findById(new QuestionId(questionId))
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        Question replacement = toQuestion(existing.questionBankId(), command);
        existing.revise(replacement.number(), replacement.stem(), replacement.options(), replacement.answerKey(),
                replacement.type(), replacement.analysis());
        questions.save(existing);
        return existing;
    }

    @Override
    @Transactional
    public Question changeStatus(long questionId, QuestionStatus status) {
        Question existing = questions.findById(new QuestionId(questionId))
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        existing.changeStatus(status);
        questions.updateStatus(existing.id(), status);
        return existing;
    }

    @Override
    @Transactional
    public void deleteQuestion(long questionId) {
        if (!questions.deleteById(new QuestionId(questionId)))
            throw new QuestionNotFoundException(questionId);
    }

    @Override
    @Transactional
    public void deleteQuestionBank(long bankId) {
        QuestionBankId id = new QuestionBankId(bankId);
        if (banks.findById(id).isEmpty())
            throw new QuestionBankNotFoundException(bankId);
        questions.deleteByBankId(id);
        banks.deleteById(id);
    }

    private static Question toQuestion(QuestionBankId bankId, QuestionCommand command) {
        try {
            List<QuestionOption> options = command.options().stream()
                    .map(o -> new QuestionOption(o.label(), o.content())).toList();
            return Question.create(bankId, new QuestionNumber(command.number()), command.stem(), options,
                    new AnswerKey(command.answers()), command.questionType(), command.analysis());
        } catch (RuntimeException e) {
            throw new QuestionValidationException(e.getMessage());
        }
    }
}
