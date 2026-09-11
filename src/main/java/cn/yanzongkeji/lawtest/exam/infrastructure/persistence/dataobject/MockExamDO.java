package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
@TableName("mock_exam")
public class MockExamDO {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String title;

  @TableField("duration_minutes")
  private Integer durationMinutes;

  @TableField("passing_score")
  private BigDecimal passingScore;

  private String status;

  @TableField("created_at")
  private Instant createdAt;

  @TableField("published_at")
  private Instant publishedAt;
}
