package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
@TableName("mock_exam_attempt")
public class MockExamAttemptDO {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("exam_id")
  private Long examId;

  @TableField("user_id")
  private Long userId;

  @TableField("started_at")
  private Instant startedAt;

  @TableField("expires_at")
  private Instant expiresAt;

  @TableField("submitted_at")
  private Instant submittedAt;

  private String status;
  private BigDecimal score;
  private Boolean passed;
}
