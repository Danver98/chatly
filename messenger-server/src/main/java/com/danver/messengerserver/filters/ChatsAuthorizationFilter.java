package com.danver.messengerserver.filters;

import com.danver.messengerserver.services.permission.PermissionService;
import com.danver.messengerserver.services.permission.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.danver.messengerserver.services.permission.PermissionType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.danver.messengerserver.utils.Constants.X_REQUEST_RESOURCE_OBJECT;

@Slf4j
@Component
public class ChatsAuthorizationFilter extends OncePerRequestFilter {

    private final PermissionService permissionService;
    @Value("${application.authorization.enabled}")
    private boolean appAuthorizationEnabled;

    @Autowired
    public ChatsAuthorizationFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String contentType = request.getContentType();
        // Multipart data requests shouldn't be wrapped
        boolean isMultipart = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
        // If there is a "GET" request with no body, do not wrap
        boolean hasBody = "POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod());

        HttpServletRequest requestToUse = (isMultipart || !hasBody)
                ? request
                : new CachedRequestWrapper(request);

        if (!isAuthorized(requestToUse)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(requestToUse, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!appAuthorizationEnabled) return true;
        String path = request.getRequestURI();
        if (!path.startsWith("/chats/")) return true;
        // Never touch the attachment endpoint's body
        return path.equals("/chats/attachment");
    }

    private boolean isAuthorized(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        boolean isJson = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("application/json");

        Map<String, Object> requestBody = Map.of();
        if (isJson) {
            // Only read the body for JSON — the wrapper guarantees it's re-readable
            byte[] data = StreamUtils.copyToByteArray(request.getInputStream());
            try {
                requestBody = new ObjectMapper().readValue(data, Map.class);
            } catch (IOException ignored) {
                // malformed JSON — fall through, other checks may still apply
            }
        }

        String methodName = request.getMethod();
        String path = request.getServletPath();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Long chatId = null;

        // 1. Prefer the resource header (works for both JSON and multipart)
        String resourceHeader = request.getHeader(X_REQUEST_RESOURCE_OBJECT);
        if (resourceHeader != null && !resourceHeader.isBlank()) {
            try {
                chatId = Long.parseLong(resourceHeader);
            } catch (NumberFormatException ignored) { }
        }

        // 2. Fall back to body field (JSON only)
        if (chatId == null && requestBody.get("chatId") != null) {
            try {
                chatId = Long.parseLong(String.valueOf(requestBody.get("chatId")));
            } catch (NumberFormatException ignored) { }
        }

        // 3. Fall back to request parameter (multipart / query string)
        if (chatId == null) {
            String param = request.getParameter("chatId");
            if (param != null && !param.isBlank()) {
                try {
                    chatId = Long.parseLong(param);
                } catch (NumberFormatException ignored) { }
            }
        }

        String permission = PermissionType.Chat.DEFAULT.getValue();
        int resourceType = ResourceType.CHAT.getValue();

        if ("POST".equals(methodName)
                && (path.equals("/chats/") || path.equals("/chats/light-list"))) {
            return true;
        }

        if (path.matches("^/chats/(\\d+).*")) {
            chatId = Long.parseLong(path.split("/")[2]);
        } else if ("POST".equals(methodName) && path.equals("/chats/create")) {
            permission = PermissionType.Chat.CREATE.getValue();
            chatId = 0L;
        } else if ("POST".equals(methodName) && path.equals("/chats/attachment")) {
            permission = PermissionType.Chat.SEND.getValue();
            // chatId already resolved from header or @RequestParam above
        }

        return permissionService.isAuthorized(
                (UserDetails) authentication.getPrincipal(),
                chatId, resourceType, permission);
    }
}
