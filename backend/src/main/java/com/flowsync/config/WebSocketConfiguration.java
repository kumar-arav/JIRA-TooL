package com.flowsync.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import com.flowsync.repository.UserRepository;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final UserRepository userRepository;

    public WebSocketConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessions.add(session);
                String email = getEmailFromSession(session);
                if (email != null) {
                    try {
                        userRepository.findByEmail(email).ifPresent(user -> {
                            user.setActive(true);
                            user.setLastLoginTime(java.time.LocalDateTime.now());
                            userRepository.save(user);
                            broadcast("{\"type\": \"USER_LOGIN\", \"user\": \"" + email + "\", \"lastLoginTime\": \"" + user.getLastLoginTime().toString() + "\"}");
                        });
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                sessions.remove(session);
                String email = getEmailFromSession(session);
                if (email != null) {
                    try {
                        userRepository.findByEmail(email).ifPresent(user -> {
                            user.setActive(false);
                            user.setLastLogoutTime(java.time.LocalDateTime.now());
                            userRepository.save(user);
                            broadcast("{\"type\": \"USER_LOGOUT\", \"user\": \"" + email + "\", \"lastLogoutTime\": \"" + user.getLastLogoutTime().toString() + "\"}");
                        });
                    } catch (Exception ignored) {}
                }
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                broadcast(message.getPayload());
            }

            private String getEmailFromSession(WebSocketSession session) {
                URI uri = session.getUri();
                if (uri == null) return null;
                String query = uri.getQuery();
                if (query == null) return null;
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && "email".equals(pair[0])) {
                        try {
                            return java.net.URLDecoder.decode(pair[1], "UTF-8");
                        } catch (Exception e) {
                            return pair[1];
                        }
                    }
                }
                return null;
            }
        }, "/ws", "/api/ws").setAllowedOrigins("*");
    }

    public static void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    sessions.remove(session);
                }
            }
        }
    }
}
