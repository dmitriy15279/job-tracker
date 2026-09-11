package com.example.jobtracker.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Tracks which user logins are currently connected via WebSocket.
 * The live {@link WebSocketSession} objects only make sense on the instance that holds them, so they
 * are kept in a local in-memory map; Redis only stores the "is this login online" flag so that a Kafka
 * consumer (potentially on a different instance) can check connectivity before attempting delivery.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    private static final String ONLINE_KEY_PREFIX = "ws:online:";

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public WebSocketSessionRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void register(String login, WebSocketSession session) {
        sessions.put(login, session);
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + login, session.getId());
        log.info("User '{}' connected via WebSocket (session={})", login, session.getId());
    }

    public void unregister(String login) {
        sessions.remove(login);
        redisTemplate.delete(ONLINE_KEY_PREFIX + login);
        log.info("User '{}' disconnected from WebSocket", login);
    }

    public boolean isOnline(String login) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + login));
    }

    public void sendMessage(String login, String message) {
        WebSocketSession session = sessions.get(login);
        if (session == null || !session.isOpen()) {
            log.warn("Redis reports '{}' as online but no local open WebSocket session was found", login);
            return;
        }
        try {
            session.sendMessage(new TextMessage(message));
            log.info("Delivered WebSocket message to '{}'", login);
        } catch (IOException e) {
            log.error("Failed to send WebSocket message to '{}'", login, e);
        }
    }
}
