package com.agent.consultant.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 经验池实体类
 */
@Data
public class AgentExperience {
    private Long id;
    private String errorKeyword; // 报错提取关键字
    private String solution;     // Agent 总结的解决方案
    private LocalDateTime createTime;
}