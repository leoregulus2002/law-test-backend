package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionAnswerDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionOptionDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionListItemDO;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionListFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 题目查询侧 Mapper，集中承载分页、统计、游标和随机查询 SQL。 */
@Mapper
public interface QuestionQueryMapper {

    List<QuestionListItemDO> findPage(@Param("offset") int offset, @Param("limit") int limit,
            @Param("filter") QuestionListFilter filter);

    long count(@Param("filter") QuestionListFilter filter);

    /** 一次查询页面内全部选项，避免逐题加载。 */
    List<QuestionOptionDO> findOptionsByQuestionIds(@Param("questionIds") List<Long> questionIds);

    /** 一次查询页面内全部答案，避免逐题加载。 */
    List<QuestionAnswerDO> findAnswersByQuestionIds(@Param("questionIds") List<Long> questionIds);

    /** 统计全部或指定题库范围内的题目数量。 */
    long countByBankIds(@Param("questionBankIds") List<Long> questionBankIds);

    /** 按主键游标读取全部或指定题库范围内的题目 ID。 */
    List<Long> findIdsAfter(@Param("questionBankIds") List<Long> questionBankIds, @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /** 使用 PostgreSQL 随机排序选取一个题目 ID。 */
    Long findRandomId(@Param("questionBankIds") List<Long> questionBankIds);
}
