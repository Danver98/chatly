package com.danver.messengerserver.filters;

import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.services.interfaces.UserService;
import com.danver.messengerserver.utils.Constants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.danver.messengerserver.utils.Constants.ACCESS_TOKEN_BLACKLIST_KEY;

/**
 * This filter should only be applicable after authentication/login process's completed
 */
@Slf4j
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public JwtTokenFilter(JwtUtil jwtUtil, UserService userService, RedisTemplate<String, String> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtUtil.resolveToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (redisTemplate.opsForHash().hasKey(ACCESS_TOKEN_BLACKLIST_KEY, token)) {
            // Access token is in blacklist
            filterChain.doFilter(request, response);
            return;
        };
        Claims claims = jwtUtil.getClaims(token);

        UserDetails userDetails = userService.loadUserByUsername((String) claims.get(Constants.USER_JWT_LOGIN_KEY));
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }
}
