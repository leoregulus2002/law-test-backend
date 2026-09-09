package cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_question_progress")
public class UserQuestionProgressDO {
    @TableField("user_id")
    private Long userId;
    @TableField("question_id")
    private Long questionId;
    private String status;
}
