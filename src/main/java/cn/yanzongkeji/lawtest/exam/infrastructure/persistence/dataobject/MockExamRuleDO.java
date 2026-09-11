package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("mock_exam_rule")
public class MockExamRuleDO {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("exam_id")
  private Long examId;

  @TableField("question_type")
  private String questionType;

  @TableField("selection_mode")
  private String selectionMode;

  @TableField("question_count")
  private Integer questionCount;

  private BigDecimal score;
}
