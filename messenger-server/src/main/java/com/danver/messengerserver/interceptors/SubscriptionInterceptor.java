package com.danver.messengerserver.interceptors;

import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.services.permission.PermissionService;
import com.danver.messengerserver.services.permission.PermissionType;
import com.danver.messengerserver.services.permission.ResourceType;
import com.danver.messengerserver.utils.Constants;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SubscriptionInterceptor implements ChannelInterceptor {

    public static final String PUBLIC_CHAT_REGEX = "^/topic/chats/(\\d+)/messages$";
    public static final String PRIVATE_QUEUE_REGEX = "^/user/(\\d+)/queue/chats/messages$";
    private final PermissionService permissionService;
    private final JwtUtil jwtUtil;

    private final UserService userService;

    @Autowired
    public SubscriptionInterceptor(PermissionService permissionService, JwtUtil jwtUtil,
                                   UserService userService) {
        this.permissionService = permissionService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            this.onConnect(accessor);
        }

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            this.onSubscribe(accessor);
        }

        return message;
    }

    private void onConnect(StompHeaderAccessor accessor) throws AccessDeniedException {
        // Extract token from Authorization header
        String token = accessor.getFirstNativeHeader("Authorization");
        log.debug("CONNECT command received, token present: {}", token != null);
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            throw new AccessDeniedException("No Authorization header provided");
        }
        if (!jwtUtil.validateToken(token)) throw new AccessDeniedException("Invalid authentication token");

        Claims claims = jwtUtil.getClaims(token);
        try {
            UserDetails userDetails = userService.loadUserByUsername((String) claims.get(Constants.USER_JWT_LOGIN_KEY));
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            // Setting user for future requests usage
            accessor.setUser((Principal) userDetails);
            log.debug("User set on CONNECT: {}", auth.getName());
        } catch (Exception e) {
            log.error("Authentication failed during CONNECT", e);
            throw new AccessDeniedException("Invalid authentication token");
        }
    }

    private void onSubscribe(StompHeaderAccessor accessor) throws AccessDeniedException {
        User user = (User) accessor.getUser();
        String destination = accessor.getDestination();

        log.debug("SUBSCRIBE to {} by user: {}", destination,
                user != null ? user.getFullName() : "null");

        if (!isUserAllowedToSubscribe(user, destination)) {
            throw new AccessDeniedException("Unauthorized subscription attempt.");
        }
    }

    private boolean isUserAllowedToSubscribe(User user, String destination) {
        if (user == null || destination == null) {
            return false;
        }

        // Firstly, check if user's trying to subscribe to his own private queue
        Pattern pattern = Pattern.compile(PRIVATE_QUEUE_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(destination);
        long userId;
        if (matcher.find()) {
            try {
                userId = Long.parseLong(matcher.group(1));
                return userId == user.getId();
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // Secondly, check whether user's trying to access some public chat
        pattern = Pattern.compile(PUBLIC_CHAT_REGEX, Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(destination);
        long chatId;
        if (matcher.find()) {
            try {
                chatId = Long.parseLong(matcher.group(1));
                // TODO: implement permission hierarchy so that admin inherits for example DEFAULT rights
                return (permissionService.isAuthorized(user, chatId, ResourceType.CHAT.getValue(),
                        PermissionType.Chat.DEFAULT.getValue()) ||
                        permissionService.isAuthorized(user, chatId, ResourceType.CHAT.getValue(),
                                PermissionType.Chat.ADMIN.getValue())
                        );
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}