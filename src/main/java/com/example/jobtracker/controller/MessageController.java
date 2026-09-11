package com.example.jobtracker.controller;

import com.example.jobtracker.controller.dto.SendMessageRequest;
import com.example.jobtracker.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> send(@Valid @RequestBody SendMessageRequest request) {
        log.info("POST /api/messages login='{}'", request.login());
        service.send(request);
        return ResponseEntity.accepted().build();
    }
}
