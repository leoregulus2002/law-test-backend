package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.application.exception.WordFileValidationException;
import cn.yanzongkeji.lawtest.question.application.exception.WordQuestionParseException;
import cn.yanzongkeji.lawtest.question.application.importing.ImportWordQuestionsUseCase;
import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.QuestionResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.WordParseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Tag(name = "题目 Word 导入", description = "上传固定表格格式的 .docx，解析后幂等导入题库")
@RestController
@RequestMapping("/api/v1/questions")
public class WordQuestionController {

    private final ImportWordQuestionsUseCase importWordQuestionsUseCase;

    public WordQuestionController(ImportWordQuestionsUseCase importWordQuestionsUseCase) {
        this.importWordQuestionsUseCase = importWordQuestionsUseCase;
    }

    @Operation(summary = "导入题目 Word", description = "仅支持包含题号、题目、选项、答案、解析五个字段的 .docx 表格；相同文件内容不会重复导入")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "解析成功", content = @Content(schema = @Schema(implementation = WordParseResponse.class))),
            @ApiResponse(responseCode = "400", description = "文件或题目表格格式不符合要求") })
    @PostMapping(value = "/parse-word", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WordParseResponse parseWord(
            @Parameter(name = "file", description = "待解析的 .docx 文件", in = ParameterIn.DEFAULT, required = true) @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new WordFileValidationException("上传文件不能为空");
        }

        try (InputStream content = file.getInputStream()) {
            ImportWordQuestionsUseCase.ImportResult imported = importWordQuestionsUseCase
                    .importWord(file.getOriginalFilename(), content);
            List<Question> questions = imported.questions();
            List<QuestionResponse> questionResponses = questions.stream().map(QuestionResponse::from).toList();
            return new WordParseResponse(file.getOriginalFilename(), imported.questionBankId(),
                    imported.questionBankCode(), imported.imported(), questionResponses.size(), questionResponses);
        } catch (IOException exception) {
            throw new WordQuestionParseException("无法读取上传的 Word 文件", exception);
        }
    }
}
