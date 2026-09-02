package cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_answer")
public class QuestionAnswerDO {
    private Long questionId;
    private String optionLabel;
}
