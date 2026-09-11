package cn.yanzongkeji.lawtest.question.application.highfrequency;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.HighFrequencyTopicDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.HighFrequencyTopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HighFrequencyTopicService {
    private final HighFrequencyTopicMapper mapper;

    public List<HighFrequencyTopicDO> list() {
        return mapper.selectList(new QueryWrapper<HighFrequencyTopicDO>()
                .orderByDesc("created_at"));
    }

    public HighFrequencyTopicDO random() {
        return mapper.selectRandom();
    }

    @Transactional
    public HighFrequencyTopicDO create(HighFrequencyTopicCommand command) {
        HighFrequencyTopicDO topic = toDataObject(command);
        mapper.insert(topic);
        return topic;
    }

    @Transactional
    public HighFrequencyTopicDO update(long id, HighFrequencyTopicCommand command) {
        if (!exists(id))
            throw new HighFrequencyTopicNotFoundException(id);
        HighFrequencyTopicDO topic = toDataObject(command);
        mapper.update(null, new LambdaUpdateWrapper<HighFrequencyTopicDO>()
                .eq(HighFrequencyTopicDO::getId, id)
                .set(HighFrequencyTopicDO::getTitle, topic.getTitle())
                .set(HighFrequencyTopicDO::getSummary, topic.getSummary())
                .set(HighFrequencyTopicDO::getCategory, topic.getCategory())
                .setSql("updated_at=current_timestamp"));
        topic.setId(id);
        return topic;
    }

    @Transactional
    public void delete(long id) {
        if (mapper.deleteById(id) == 0)
            throw new HighFrequencyTopicNotFoundException(id);
    }

    private boolean exists(long id) {
        return id > 0 && mapper.selectById(id) != null;
    }

    private static HighFrequencyTopicDO toDataObject(HighFrequencyTopicCommand command) {
        HighFrequencyTopicDO topic = new HighFrequencyTopicDO();
        topic.setTitle(requireText(command.title(), "考点标题不能为空", 128));
        topic.setSummary(requireText(command.summary(), "考点说明不能为空", 10_000));
        topic.setCategory(requireText(command.category(), "考点分类不能为空", 64));
        return topic;
    }

    private static String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(message);
        String normalized = value.strip();
        if (normalized.length() > maxLength)
            throw new IllegalArgumentException(message.replace("不能为空", "不能超过 " + maxLength + " 个字符"));
        return normalized;
    }
}
