package com.example.jobtracker.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String LOGIN_ATTRIBUTE = "login";

    private final WebSocketSessionRegistry registry;

    public ChatWebSocketHandler(WebSocketSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String login = extractLogin(session);
        if (login == null || login.isBlank()) {
            log.warn("Rejecting WebSocket connection without a 'login' query parameter");
            session.close(CloseStatus.BAD_DATA.withReason("Missing 'login' query parameter"));
            return;
        }
        session.getAttributes().put(LOGIN_ATTRIBUTE, login);
        registry.register(login, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String login = (String) session.getAttributes().get(LOGIN_ATTRIBUTE);
        if (login != null) {
            registry.unregister(login);
        }
    }

    private String extractLogin(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("login");
    }
}
