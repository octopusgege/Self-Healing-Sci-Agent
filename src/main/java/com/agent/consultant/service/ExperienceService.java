package com.agent.consultant.service;

import com.agent.consultant.mapper.ExperienceMapper;
import com.agent.consultant.pojo.AgentExperience;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExperienceService {

    @Autowired
    private ExperienceMapper mapper;
    
    /**
     * 获取修复建议
     */
    public String getAdvice(String errorMsg) {
        AgentExperience exp = mapper.findSolution(errorMsg);
        if (exp != null && exp.getSolution() != null && !exp.getSolution().trim().isEmpty()) {
            return exp.getSolution();
        }
        return "未匹配到历史经验，请自主分析排查。";
    }
    
    /**
     * 记录成功经验
     */
    public void record(String keyword, String solution) {
        if (keyword == null || keyword.trim().isEmpty() || solution == null || solution.trim().isEmpty()) {
            throw new IllegalArgumentException("关键字和解决方案不能为空");
        }
        AgentExperience exp = new AgentExperience();
        exp.setErrorKeyword(keyword);
        exp.setSolution(solution);
        mapper.saveExperience(exp);
    }
}