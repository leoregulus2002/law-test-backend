package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/** 系统配置字典中的一项。 */
@Data
@TableName("system_configuration")
public class SystemConfigurationDO {
  @TableId("config_key")
  private String key;

  @TableField("config_value")
  private String value;

  @TableField("updated_at")
  private Instant updatedAt;
}
