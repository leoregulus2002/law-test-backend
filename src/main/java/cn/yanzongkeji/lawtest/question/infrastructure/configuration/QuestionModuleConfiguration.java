package cn.yanzongkeji.lawtest.question.infrastructure.configuration;

import cn.yanzongkeji.lawtest.question.application.command.*;
import cn.yanzongkeji.lawtest.question.application.importing.*;
import cn.yanzongkeji.lawtest.question.application.query.*;
import cn.yanzongkeji.lawtest.question.domain.port.*;
import cn.yanzongkeji.lawtest.question.infrastructure.word.ApachePoiWordQuestionParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 题目上下文的组合根。应用层与领域层不依赖 Spring。
 */
@Configuration
public class QuestionModuleConfiguration {

    @Bean
    WordQuestionParser wordQuestionParser() {
        return new ApachePoiWordQuestionParser();
    }

    @Bean
    ParseWordQuestionsUseCase parseWordQuestionsUseCase(WordQuestionParser wordQuestionParser) {
        return new ParseWordQuestionsService(wordQuestionParser);
    }

    @Bean
    ImportWordQuestionsUseCase importWordQuestionsUseCase(WordQuestionParser parser, QuestionBankRepository banks,
            QuestionRepository questions) {
        return new ImportWordQuestionsService(parser, banks, questions);
    }

    @Bean
    QuestionManagementUseCase questionManagementUseCase(QuestionBankRepository banks, QuestionRepository questions) {
        return new QuestionManagementService(banks, questions);
    }

    @Bean
    QuestionQueryUseCase questionQueryUseCase(QuestionBankRepository banks, QuestionRepository questions,
            QuestionManagementQuery managementQuery, QuestionPracticeQuery practiceQuery) {
        return new QuestionQueryService(banks, questions, managementQuery, practiceQuery);
    }
}
