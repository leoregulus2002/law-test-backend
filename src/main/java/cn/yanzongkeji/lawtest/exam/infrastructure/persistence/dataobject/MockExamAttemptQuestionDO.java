package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("mock_exam_attempt_question")
public class MockExamAttemptQuestionDO {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("attempt_id")
  private Long attemptId;

  @TableField("question_id")
  private Long questionId;

  @TableField("order_no")
  private Integer orderNo;

  @TableField("question_type")
  private String questionType;

  private BigDecimal score;

  @TableField("correct_answer")
  private String correctAnswer;

  @TableField("reference_answer")
  private String referenceAnswer;

  @TableField("selected_answer")
  private String selectedAnswer;

  @TableField("subjective_answer")
  private String subjectiveAnswer;

  @TableField("awarded_score")
  private BigDecimal awardedScore;
}
