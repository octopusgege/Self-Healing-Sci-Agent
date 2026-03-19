package com.agent.consultant.controller;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QKD 知识库管理 Controller
 * 负责文献上传与动态 RAG 向量化注入
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/qkd")
public class QkdKnowledgeController {

    @Autowired
    @Qualifier("store")  // 注入 CommonConfig 中定义的 EmbeddingStore Bean
    private EmbeddingStore embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 知识库文件存储目录，可通过配置文件自定义
     */
    @Value("${qkd.knowledge.base-dir:./knowledge-base}")
    private String knowledgeBaseDir;

    /**
     * 文档分块大小
     */
    @Value("${qkd.knowledge.chunk-size:500}")
    private int chunkSize;

    /**
     * 文档分块重叠大小
     */
    @Value("${qkd.knowledge.chunk-overlap:100}")
    private int chunkOverlap;

    /**
     * 上传 PDF 文献并动态注入 RAG 向量数据库
     *
     * @param pdfFile 上传的 PDF 文献文件
     * @return 包含上传状态和文件信息的 JSON
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadPaper(@RequestParam("pdfFile") MultipartFile pdfFile) {
        log.info("=== 开始处理文献上传请求 ===");
        Map<String, Object> result = new HashMap<>();

        // 1. 参数校验
        if (pdfFile == null || pdfFile.isEmpty()) {
            log.error("上传文件为空");
            result.put("success", false);
            result.put("message", "请选择要上传的 PDF 文件");
            return result;
        }

        String originalFilename = pdfFile.getOriginalFilename();
        log.info("原始文件名：{}", originalFilename);
        log.info("文件大小：{} bytes", pdfFile.getSize());
        log.info("文件类型：{}", pdfFile.getContentType());
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            result.put("success", false);
            result.put("message", "仅支持 PDF 格式的文件");
            return result;
        }

        try {
            // 2. 创建知识库目录（如果不存在）
            Path basePath = Paths.get(knowledgeBaseDir);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("创建知识库目录: {}", basePath.toAbsolutePath());
            }

            // 3. 生成唯一文件名（防止重名覆盖）
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeFilename = timestamp + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
            Path targetPath = basePath.resolve(safeFilename);

            // 4. 保存文件到本地
            Files.copy(pdfFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF 文件已保存: {}", targetPath.toAbsolutePath());

            // 5. 使用 LangChain4j 加载 PDF 文档
            Document document = FileSystemDocumentLoader.loadDocument(
                    targetPath,
                    new ApachePdfBoxDocumentParser()
            );
            log.info("PDF 文档加载成功，内容长度: {} 字符", document.text().length());

            // 6. 构建文档分割器
            DocumentSplitter documentSplitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);

            // 7. 构建 Ingestor 并执行向量化注入
            log.info("开始向量化处理...");
            log.info("EmbeddingStore: {}", embeddingStore != null ? embeddingStore.getClass().getSimpleName() : "null");
            log.info("EmbeddingModel: {}", embeddingModel != null ? embeddingModel.getClass().getSimpleName() : "null");
            
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .documentSplitter(documentSplitter)
                    .build();

            // 8. 将文档向量化并存入向量数据库
            log.info("开始执行向量化注入...");
            ingestor.ingest(document);
            log.info("文档已成功向量化并存入 RAG 知识库");

            // 9. 返回成功结果
            result.put("success", true);
            result.put("message", "文献上传并向量化成功");
            result.put("fileName", originalFilename);
            result.put("savedPath", targetPath.toAbsolutePath().toString());
            result.put("documentLength", document.text().length());
            log.info("=== 文献上传成功完成 ===");

        } catch (IOException e) {
            log.error("文件保存失败", e);
            result.put("success", false);
            result.put("message", "文件保存失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文档向量化处理失败 - 详细错误：{}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "文档向量化处理失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 批量上传多个 PDF 文献
     *
     * @param pdfFiles 上传的多个 PDF 文件
     * @return 包含上传状态和文件信息的 JSON
     */
    @PostMapping("/upload/batch")
    public Map<String, Object> uploadPapers(@RequestParam("pdfFiles") List<MultipartFile> pdfFiles) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;

        for (MultipartFile pdfFile : pdfFiles) {
            Map<String, Object> singleResult = uploadPaper(pdfFile);
            if (Boolean.TRUE.equals(singleResult.get("success"))) {
                successCount++;
            } else {
                failCount++;
            }
        }

        result.put("success", failCount == 0);
        result.put("message", String.format("批量上传完成: 成功 %d 个, 失败 %d 个", successCount, failCount));
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        return result;
    }
}
