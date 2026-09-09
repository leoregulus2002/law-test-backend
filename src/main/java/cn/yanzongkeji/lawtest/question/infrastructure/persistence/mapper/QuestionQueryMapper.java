package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionAnswerDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionOptionDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionListItemDO;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionListFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 题目查询侧 Mapper，集中承载分页、统计、游标和随机查询 SQL。 */
@Mapper
public interface QuestionQueryMapper {

    /** 按题号和主键稳定排序后分页查询题目主表。 */
    @Select("""
            select id, question_bank_id, sequence_no, stem, analysis, question_type, status
            from question
            where question_bank_id=#{questionBankId}
            order by sequence_no, id
            limit #{limit} offset #{offset}
            """)
    List<QuestionDO> findPageByBankId(@Param("questionBankId") long questionBankId,
            @Param("offset") int offset, @Param("limit") int limit);

    @Select({
            "<script>",
            "select q.id, q.question_bank_id, b.name as question_bank_name, q.sequence_no, q.stem, q.question_type, q.status",
            "from question q join question_bank b on b.id = q.question_bank_id",
            "<where>",
            "<if test='filter.keyword != null and !filter.keyword.isBlank()'>and lower(q.stem) like concat('%', lower(#{filter.keyword}), '%')</if>",
            "<if test='filter.questionType != null'>and q.question_type = #{filter.questionType}</if>",
            "<if test='filter.status != null'>and q.status = #{filter.status}</if>",
            "<if test='filter.questionBankId != null'>and q.question_bank_id = #{filter.questionBankId}</if>",
            "</where>",
            "order by q.id desc limit #{limit} offset #{offset}",
            "</script>"
    })
    List<QuestionListItemDO> findPage(@Param("offset") int offset, @Param("limit") int limit,
            @Param("filter") QuestionListFilter filter);

    @Select({
            "<script>",
            "select count(*) from question q",
            "<where>",
            "<if test='filter.keyword != null and !filter.keyword.isBlank()'>and lower(q.stem) like concat('%', lower(#{filter.keyword}), '%')</if>",
            "<if test='filter.questionType != null'>and q.question_type = #{filter.questionType}</if>",
            "<if test='filter.status != null'>and q.status = #{filter.status}</if>",
            "<if test='filter.questionBankId != null'>and q.question_bank_id = #{filter.questionBankId}</if>",
            "</where>",
            "</script>"
    })
    long count(@Param("filter") QuestionListFilter filter);

    /** 统计指定题库的题目数量。 */
    @Select("select count(*) from question where question_bank_id=#{questionBankId}")
    long countByBankId(@Param("questionBankId") long questionBankId);

    /** 一次查询页面内全部选项，避免逐题加载。 */
    @Select({
            "<script>",
            "select id, question_id, label, content, display_order from question_option",
            "where question_id in",
            "<foreach collection='questionIds' item='questionId' open='(' separator=',' close=')'>",
            "#{questionId}",
            "</foreach>",
            "order by question_id, display_order",
            "</script>"
    })
    List<QuestionOptionDO> findOptionsByQuestionIds(@Param("questionIds") List<Long> questionIds);

    /** 一次查询页面内全部答案，避免逐题加载。 */
    @Select({
            "<script>",
            "select question_id, option_label from question_answer",
            "where question_id in",
            "<foreach collection='questionIds' item='questionId' open='(' separator=',' close=')'>",
            "#{questionId}",
            "</foreach>",
            "order by question_id, option_label",
            "</script>"
    })
    List<QuestionAnswerDO> findAnswersByQuestionIds(@Param("questionIds") List<Long> questionIds);

    /** 统计全部或指定题库范围内的题目数量。 */
    @Select({
            "<script>",
            "select count(*) from question where status = 'ACTIVE'",
            "<if test='questionBankIds != null and questionBankIds.size() > 0'>",
            "and question_bank_id in",
            "<foreach collection='questionBankIds' item='questionBankId' open='(' separator=',' close=')'>",
            "#{questionBankId}",
            "</foreach>",
            "</if>",
            "</script>"
    })
    long countByBankIds(@Param("questionBankIds") List<Long> questionBankIds);

    /** 按主键游标读取全部或指定题库范围内的题目 ID。 */
    @Select({
            "<script>",
            "select id from question",
            "<where>",
            "status = 'ACTIVE'",
            "<if test='questionBankIds != null and questionBankIds.size() > 0'>",
            "and question_bank_id in",
            "<foreach collection='questionBankIds' item='questionBankId' open='(' separator=',' close=')'>",
            "#{questionBankId}",
            "</foreach>",
            "</if>",
            "<if test='cursor != null'>and id &gt; #{cursor}</if>",
            "</where>",
            "order by id limit #{limit}",
            "</script>"
    })
    List<Long> findIdsAfter(@Param("questionBankIds") List<Long> questionBankIds, @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /** 使用 PostgreSQL 随机排序选取一个题目 ID。 */
    @Select({
            "<script>",
            "select id from question where status = 'ACTIVE'",
            "<if test='questionBankIds != null and questionBankIds.size() > 0'>",
            "and question_bank_id in",
            "<foreach collection='questionBankIds' item='questionBankId' open='(' separator=',' close=')'>",
            "#{questionBankId}",
            "</foreach>",
            "</if>",
            "order by random() limit 1",
            "</script>"
    })
    Long findRandomId(@Param("questionBankIds") List<Long> questionBankIds);
}
