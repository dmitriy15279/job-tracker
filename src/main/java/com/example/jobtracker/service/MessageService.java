package com.example.jobtracker.service;

import com.example.jobtracker.config.KafkaConfig;
import com.example.jobtracker.controller.dto.SendMessageRequest;
import com.example.jobtracker.messaging.ChatMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageService {

    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;

    public MessageService(KafkaTemplate<String, ChatMessageEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(SendMessageRequest request) {
        ChatMessageEvent event = new ChatMessageEvent(request.login(), request.message());
        kafkaTemplate.send(KafkaConfig.CHAT_MESSAGES_TOPIC, request.login(), event);
        log.info("Queued message for user '{}'", request.login());
    }
}
