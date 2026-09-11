package cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("high_frequency_topic")
public class HighFrequencyTopicDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String category;
}
