package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.HighFrequencyTopicDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HighFrequencyTopicMapper extends BaseMapper<HighFrequencyTopicDO> {
    @Select("select id, title, summary, category from high_frequency_topic order by random() limit 1")
    HighFrequencyTopicDO selectRandom();
}
