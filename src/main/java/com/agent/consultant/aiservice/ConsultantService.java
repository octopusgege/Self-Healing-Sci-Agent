package com.agent.consultant.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,//手动装配
        chatModel = "openAiChatModel",//指定模型
        streamingChatModel = "openAiStreamingChatModel",
        //chatMemory = "chatMemory",//配置会话记忆对象
        chatMemoryProvider = "chatMemoryProvider",//配置会话记忆提供者对象
        contentRetriever = "contentRetriever",//配置向量数据库检索对象
        tools = "qkdSimulationTools"  // QKD 仿真工具
)
//@AiService
public interface ConsultantService {
    //用于聊天的方法
    //public String chat(String message);
    @SystemMessage(fromResource = "system.txt")
    //@UserMessage("你是量子保密通信领域的专家")
    public Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}
