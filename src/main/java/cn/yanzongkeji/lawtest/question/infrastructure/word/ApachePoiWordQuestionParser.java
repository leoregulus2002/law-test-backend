package cn.yanzongkeji.lawtest.question.infrastructure.word;

import cn.yanzongkeji.lawtest.question.application.exception.WordQuestionParseException;
import cn.yanzongkeji.lawtest.question.domain.model.AnswerKey;
import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionNumber;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionOption;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;
import cn.yanzongkeji.lawtest.question.domain.port.WordQuestionParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/** Apache POI 对 Word 题目表格格式的基础设施适配器。 */
public final class ApachePoiWordQuestionParser implements WordQuestionParser {

  private static final Set<String> OBJECTIVE_FIELDS = Set.of("题号", "题目", "选项", "答案", "解析");
  private static final Set<String> SUBJECTIVE_FIELDS = Set.of("题号", "题目", "答案");
  private static final Pattern OPTION_PATTERN = Pattern.compile("^\\s*([A-Z])[.．、)]\\s*(.+?)\\s*$");
  private static final Pattern ANSWER_LABEL_PATTERN = Pattern.compile("[A-Z]");

  @Override
  public List<Question> parse(InputStream content, QuestionType questionType) throws IOException {
    try (XWPFDocument document = new XWPFDocument(content)) {
      List<Question> questions = new ArrayList<>();
      List<XWPFTable> tables = document.getTables();
      for (int index = 0; index < tables.size(); index++) {
        int wordTableIndex = index + 1;
        Map<String, String> fields = extractFields(tables.get(index), wordTableIndex, questionType);
        if (fields.isEmpty()) {
          continue;
        }
        questions.add(toQuestion(fields, wordTableIndex, questionType));
      }
      if (questions.isEmpty()) {
        throw new WordQuestionParseException("未找到符合题型要求的题目表格");
      }
      return List.copyOf(questions);
    } catch (WordQuestionParseException exception) {
      throw exception;
    } catch (POIXMLException | IllegalArgumentException exception) {
      throw new WordQuestionParseException("Word 文件格式无效: " + exception.getMessage(), exception);
    }
  }

  private Map<String, String> extractFields(
      XWPFTable table, int wordTableIndex, QuestionType questionType) {
    Set<String> requiredFields =
        questionType == QuestionType.SUBJECTIVE ? SUBJECTIVE_FIELDS : OBJECTIVE_FIELDS;
    Map<String, String> fields = new LinkedHashMap<>();
    for (XWPFTableRow row : table.getRows()) {
      List<XWPFTableCell> cells = row.getTableCells();
      if (cells.size() < 2) {
        continue;
      }
      String label = readCellText(cells.getFirst()).strip();
      if (!requiredFields.contains(label)) {
        continue;
      }
      if (fields.putIfAbsent(label, readCellText(cells.get(1))) != null) {
        throw tableError(wordTableIndex, "字段“" + label + "”重复");
      }
    }

    if (fields.isEmpty()) {
      return fields;
    }
    Set<String> missingFields = new LinkedHashSet<>(requiredFields);
    missingFields.removeAll(fields.keySet());
    if (!missingFields.isEmpty()) {
      throw tableError(wordTableIndex, "缺少字段: " + String.join("、", missingFields));
    }
    return fields;
  }

  private Question toQuestion(
      Map<String, String> fields, int wordTableIndex, QuestionType questionType) {
    try {
      int number = Integer.parseInt(requiredText(fields, "题号"));
      String stem = requiredText(fields, "题目");
      if (questionType == QuestionType.SUBJECTIVE) {
        return Question.create(
            new QuestionNumber(number),
            stem,
            List.of(),
            new AnswerKey(List.of()),
            questionType,
            requiredText(fields, "答案"));
      }
      List<QuestionOption> options = parseOptions(requiredText(fields, "选项"), wordTableIndex);
      AnswerKey answerKey = new AnswerKey(parseAnswerLabels(requiredText(fields, "答案")));
      return Question.create(
          new QuestionNumber(number), stem, options, answerKey, questionType, fields.get("解析"));
    } catch (NumberFormatException exception) {
      throw tableError(wordTableIndex, "题号必须是正整数");
    } catch (IllegalArgumentException exception) {
      throw tableError(wordTableIndex, exception.getMessage());
    }
  }

  private List<QuestionOption> parseOptions(String optionsText, int wordTableIndex) {
    List<QuestionOption> options = new ArrayList<>();
    for (String line : optionsText.split("\\R")) {
      String normalizedLine = line.strip();
      if (normalizedLine.isEmpty()) {
        continue;
      }
      Matcher matcher = OPTION_PATTERN.matcher(normalizedLine);
      if (matcher.matches()) {
        options.add(new QuestionOption(matcher.group(1), matcher.group(2)));
        continue;
      }
      if (options.isEmpty()) {
        throw tableError(wordTableIndex, "选项必须以 A.、B. 等标识开头");
      }
      QuestionOption previousOption = options.removeLast();
      options.add(
          new QuestionOption(
              previousOption.label(), previousOption.content() + "\n" + normalizedLine));
    }
    if (options.isEmpty()) {
      throw tableError(wordTableIndex, "未解析到任何选项");
    }
    return List.copyOf(options);
  }

  private List<String> parseAnswerLabels(String answerText) {
    Matcher matcher = ANSWER_LABEL_PATTERN.matcher(answerText.toUpperCase(Locale.ROOT));
    List<String> answerLabels = new ArrayList<>();
    while (matcher.find()) {
      answerLabels.add(matcher.group());
    }
    return List.copyOf(answerLabels);
  }

  private String requiredText(Map<String, String> fields, String fieldName) {
    String value = fields.get(fieldName);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("字段“" + fieldName + "”不能为空");
    }
    return value.strip();
  }

  private String readCellText(XWPFTableCell cell) {
    return cell.getParagraphs().stream()
        .map(XWPFParagraph::getText)
        .map(String::strip)
        .filter(text -> !text.isEmpty())
        .collect(Collectors.joining("\n"));
  }

  private WordQuestionParseException tableError(int wordTableIndex, String message) {
    return new WordQuestionParseException("第 " + wordTableIndex + " 个表格: " + message);
  }
}
