package com.example.jobtracker.messaging;

import com.example.jobtracker.config.KafkaConfig;
import com.example.jobtracker.websocket.WebSocketSessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatMessageListener {

    private final WebSocketSessionRegistry registry;

    public ChatMessageListener(WebSocketSessionRegistry registry) {
        this.registry = registry;
    }

    @KafkaListener(topics = KafkaConfig.CHAT_MESSAGES_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ChatMessageEvent event) {
        log.info("Consumed chat message for user '{}'", event.login());
        if (!registry.isOnline(event.login())) {
            log.info("User '{}' is not connected - dropping message", event.login());
            return;
        }
        registry.sendMessage(event.login(), event.message());
    }
}
