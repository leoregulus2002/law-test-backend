package cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question_option")
public class QuestionOptionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("question_id")
    private Long questionId;
    private String label;
    private String content;
    @TableField("display_order")
    private Short displayOrder;
}
