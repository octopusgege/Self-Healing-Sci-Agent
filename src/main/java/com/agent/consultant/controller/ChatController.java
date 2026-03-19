package com.agent.consultant.controller;

import com.agent.consultant.aiservice.ConsultantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    @Autowired
    private ConsultantService consultantService;

    // ⚠️ 修复关键点：将 produces 从 text/html 修改为 text/event-stream，以支持前端流式读取
    @RequestMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=utf-8")
    public Flux<String> chat(String memoryId, String message){
        return consultantService.chat(memoryId, message);
    }
}