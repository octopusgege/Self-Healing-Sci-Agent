package com.agent.consultant.config;

import com.agent.consultant.repository.RedisChatMemoryStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 核心配置类，负责装配记忆提供者(Memory)与向量知识库(RAG)
 */
@Slf4j
@Configuration
public class CommonConfig {

    @Autowired
    private OpenAiChatModel model;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    // 本地向量缓存文件路径，避免每次启动消耗 Token 重新计算
    private static final String EMBEDDING_CACHE_FILE = "qkd_embeddings_cache.json";

    /**
     * 配置会话记忆提供者
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(30) // 保留最近 30 条消息，平衡上下文深度与 Token 消耗
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    /**
     * 配置 RAG 向量数据库
     * 采用本地序列化缓存策略，命中缓存则秒级启动
     */
    @Bean
    public EmbeddingStore<TextSegment> store(){
        Path path = Paths.get(EMBEDDING_CACHE_FILE);
    
        // 1. 尝试读取本地缓存
        if (Files.exists(path)) {
            log.info("命中本地向量缓存文件，直接加载：{}", EMBEDDING_CACHE_FILE);
            try {
                return InMemoryEmbeddingStore.fromFile(EMBEDDING_CACHE_FILE);
            } catch (Exception e) {
                log.warn("缓存文件加载失败，将重新生成：{}", e.getMessage());
                // 删除旧的缓存文件
                try {
                    Files.deleteIfExists(path);
                    log.info("已删除旧的缓存文件");
                } catch (IOException ex) {
                    log.error("删除缓存文件失败", ex);
                }
            }
        }
    
        log.info("未命中本地缓存或缓存已失效，开始解析文献并计算向量...");

        // 2. 加载基础文献与目录映射配置
        List<Document> pdfDocuments = ClassPathDocumentLoader.loadDocuments("content", new ApachePdfBoxDocumentParser());
        PathMatcher txtMatcher = p -> p.toString().toLowerCase().endsWith(".txt");
        List<Document> txtDocuments = ClassPathDocumentLoader.loadDocuments("content", txtMatcher);

        List<Document> allDocuments = new ArrayList<>(pdfDocuments);
        allDocuments.addAll(txtDocuments);

        // 3. 构建内存向量库与分词器
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        DocumentSplitter ds = DocumentSplitters.recursive(500, 100);

        // 4. 执行向量化注入
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .documentSplitter(ds)
                .embeddingModel(embeddingModel)
                .build();
        ingestor.ingest(allDocuments);

        // 5. 序列化至本地供下次使用
        store.serializeToFile(EMBEDDING_CACHE_FILE);
        log.info("向量数据已持久化至本地: {}", EMBEDDING_CACHE_FILE);

        return store;
    }

    /**
     * 配置检索器，调节相似度阈值
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .minScore(0.3) // 相似度阈值
                .maxResults(3) // 最大召回段落数
                .embeddingModel(embeddingModel)
                .build();
    }
}