package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionBankDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBankDO> {
    @Select("insert into question_bank(code,name,source_file_name) values(#{code},#{name},#{sourceFileName}) on conflict (code) do nothing returning id")
    Long insertIfAbsent(QuestionBankDO data);

    @Select("select id from question_bank where code=#{code}")
    Long selectIdByCode(String code);
}
