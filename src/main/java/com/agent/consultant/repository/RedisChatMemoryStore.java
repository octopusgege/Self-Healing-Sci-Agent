package com.agent.consultant.repository;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Redis 的聊天记忆存储实现类
 * 用于将大模型的上下文会话持久化，防止服务重启或页面刷新导致上下文丢失
 */
@Repository
public class RedisChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Redis 存储前缀，区分不同业务
    private static final String MEMORY_KEY_PREFIX = "qkd:chat:memory:";
    // 会话保留时间（天）
    private static final int MEMORY_TTL_DAYS = 7;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(MEMORY_KEY_PREFIX + memoryId);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // 反序列化为 LangChain4j 可识别的消息列表
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        String json = ChatMessageSerializer.messagesToJson(list);
        // 更新会话并重置过期时间
        redisTemplate.opsForValue().set(MEMORY_KEY_PREFIX + memoryId, json, Duration.ofDays(MEMORY_TTL_DAYS));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(MEMORY_KEY_PREFIX + memoryId);
    }
}