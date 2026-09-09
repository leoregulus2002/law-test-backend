package cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject;

import lombok.Data;

/** 题目主表与题库名称的管理端列表投影。 */
@Data
public class QuestionListItemDO {
    private Long id;
    private Long questionBankId;
    private String questionBankName;
    private Integer sequenceNo;
    private String stem;
    private String questionType;
    private String status;
}
