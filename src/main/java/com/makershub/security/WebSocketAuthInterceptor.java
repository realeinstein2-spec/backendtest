package com.makershub.security;

import com.makershub.entity.Order;
import com.makershub.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final OrderRepository orderRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                List<String> authHeaders = accessor.getNativeHeader("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    String bearerToken = authHeaders.getFirst();
                    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                        String jwt = bearerToken.substring(7);
                        try {
                            if (jwtUtil.isTokenValid(jwt, "ACCESS")) {
                                UUID userId = jwtUtil.extractUserId(jwt);
                                String phone = jwtUtil.parseToken(jwt).get("phone", String.class);
                                UserDetails userDetails = userDetailsService.loadUserByUsername(phone);

                                if (userDetails instanceof UserDetailsImpl impl && impl.getId().equals(userId)) {
                                    UsernamePasswordAuthenticationToken auth =
                                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                    accessor.setUser(auth);
                                    log.debug("STOMP connect authorized for user phone: {}", phone);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("STOMP connection JWT validation failed: {}", e.getMessage());
                            throw new IllegalArgumentException("Unauthorized connection: " + e.getMessage());
                        }
                    }
                }

                if (accessor.getUser() == null) {
                    throw new IllegalArgumentException("Unauthorized connection: Missing or invalid token");
                }
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                Principal principal = accessor.getUser();
                if (principal == null) {
                    throw new IllegalArgumentException("Unauthorized subscription: Not authenticated");
                }

                String destination = accessor.getDestination();
                if (destination != null && destination.startsWith("/topic/orders/")) {
                    String orderIdStr = destination.substring("/topic/orders/".length());
                    try {
                        UUID orderId = UUID.fromString(orderIdStr);
                        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                        UserDetailsImpl userDetails = (UserDetailsImpl) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
                        UUID userId = userDetails.getId();

                        // Check if user is either the SME or the Factory of this order
                        if (!order.getSme().getId().equals(userId) && !order.getFactory().getId().equals(userId)) {
                            log.warn("User {} attempted unauthorized subscription to topic {}", userId, destination);
                            throw new IllegalArgumentException("Unauthorized subscription to this order's topic");
                        }
                    } catch (Exception e) {
                        log.warn("STOMP subscription validation failed for destination {}: {}", destination, e.getMessage());
                        throw new IllegalArgumentException("Unauthorized subscription: " + e.getMessage());
                    }
                }
            }
        }

        return message;
    }
}
