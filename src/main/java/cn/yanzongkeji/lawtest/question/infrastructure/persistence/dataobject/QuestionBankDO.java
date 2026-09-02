package cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question_bank")
public class QuestionBankDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    @TableField("source_file_name")
    private String sourceFileName;
}
