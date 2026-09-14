package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.UserQuestionProgressDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserQuestionProgressMapper extends BaseMapper<UserQuestionProgressDO> {
  @Insert(
      """
      insert into user_question_progress (user_id, question_id, status, answered_at)
      values (#{userId}, #{questionId}, #{status}, current_timestamp)
      on conflict (user_id, question_id) do update
      set status = excluded.status, answered_at = current_timestamp
      """)
  int upsert(
      @Param("userId") long userId,
      @Param("questionId") long questionId,
      @Param("status") String status);

  @Insert(
      """
insert into user_question_progress (user_id, question_id, status, subjective_answer, answered_at)
values (#{userId}, #{questionId}, 'ANSWERED', #{answer}, current_timestamp)
on conflict (user_id, question_id) do update
set status = 'ANSWERED', subjective_answer = excluded.subjective_answer, answered_at = current_timestamp
""")
  int upsertSubjectiveAnswer(
      @Param("userId") long userId,
      @Param("questionId") long questionId,
      @Param("answer") String answer);
}
