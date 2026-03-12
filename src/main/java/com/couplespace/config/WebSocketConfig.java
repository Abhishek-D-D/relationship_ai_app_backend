package com.couplespace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.UUID;
import com.couplespace.security.JwtUtil;
import com.couplespace.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker — topics for broadcast, queue for user-specific
        registry.enableSimpleBroker("/topic", "/queue");
        // Messages routed from client to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // User-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    log.info("STOMP CONNECT: Authorization header present: {}", (authHeader != null));

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            if (jwtUtil.isTokenValid(token)) {
                                UUID userId = jwtUtil.extractUserId(token);
                                log.info("STOMP CONNECT: Valid token for user: {}", userId);
                                userRepository.findById(userId).ifPresent(user -> {
                                    var auth = new UsernamePasswordAuthenticationToken(
                                            user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                                    accessor.setUser(auth);
                                    log.info("STOMP CONNECT: Authentication set for user: {}", user.getEmail());
                                });
                            } else {
                                log.warn("STOMP CONNECT: Invalid token");
                            }
                        } catch (Exception e) {
                            log.error("STOMP CONNECT: Token extraction error: {}", e.getMessage());
                        }
                    } else {
                        log.warn("STOMP CONNECT: Missing or invalid Authorization header format");
                    }
                }
                return message;
            }
        });
    }
}
