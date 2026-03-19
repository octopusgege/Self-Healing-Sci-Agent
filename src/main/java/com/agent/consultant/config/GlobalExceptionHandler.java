package com.agent.consultant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件大小超出限制", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "文件大小超出限制，最大支持 50MB");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(result);
    }

    /**
     * 处理 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("参数校验失败：{}", e.getMessage(), e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "参数错误：" + e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理通用 Exception
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("服务器内部错误：{}", e.getMessage(), e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "服务器内部错误：" + e.getMessage());
        return ResponseEntity.internalServerError().body(result);
    }

    /**
     * 处理 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("运行时错误：{}", e.getMessage(), e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "运行时错误：" + e.getMessage());
        return ResponseEntity.internalServerError().body(result);
    }
}
