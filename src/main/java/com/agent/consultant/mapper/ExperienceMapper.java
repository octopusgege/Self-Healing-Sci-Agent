package com.agent.consultant.mapper;

import com.agent.consultant.pojo.AgentExperience;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperienceMapper {
    // 插入新经验
    @Insert("INSERT INTO agent_experience(error_keyword, solution) VALUES(#{errorKeyword}, #{solution})")
    void saveExperience(AgentExperience exp);

    // 根据报错日志模糊匹配历史解决方案
    @Select("SELECT * FROM agent_experience WHERE #{errorMessage} LIKE CONCAT('%', error_keyword, '%') LIMIT 1")
    AgentExperience findSolution(String errorMessage);
}