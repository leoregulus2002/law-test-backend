package cn.yanzongkeji.lawtest.question.domain.model;

/** 根据正确答案选项数量确定的客观题题型。 */
public enum QuestionType {
    SINGLE_CHOICE, MULTIPLE_CHOICE;

    public static QuestionType from(AnswerKey answerKey) {
        if (answerKey.optionLabels().size() == 1) {
            return SINGLE_CHOICE;
        }
        return MULTIPLE_CHOICE;
    }
}
