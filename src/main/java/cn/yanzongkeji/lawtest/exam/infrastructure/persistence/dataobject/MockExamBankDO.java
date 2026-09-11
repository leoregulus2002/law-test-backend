package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("mock_exam_bank")
public class MockExamBankDO {
  private Long examId;
  private Long questionBankId;
}
