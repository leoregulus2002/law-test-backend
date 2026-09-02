package cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question")
public class QuestionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("question_bank_id")
    private Long questionBankId;
    @TableField("sequence_no")
    private Integer sequenceNo;
    private String stem;
    private String analysis;
    @TableField("question_type")
    private String questionType;
}
