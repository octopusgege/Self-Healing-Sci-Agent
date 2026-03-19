package com.agent.consultant;

// 注意导入这个类
import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
public class ConsultantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultantApplication.class, args);
    }

}