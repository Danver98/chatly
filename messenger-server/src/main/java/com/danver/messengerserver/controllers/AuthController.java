package com.danver.messengerserver.controllers;

import com.danver.messengerserver.auth.AuthDTO;
import com.danver.messengerserver.auth.AuthData;
import com.danver.messengerserver.auth.JwtUtil;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.services.implementations.AuthService;
import com.danver.messengerserver.services.interfaces.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static com.danver.messengerserver.utils.Constants.X_REFRESH_TOKEN_COOKIE;
import static com.danver.messengerserver.utils.Constants.X_USER_DEVICE_ID;


@Slf4j
@RestController
@RequestMapping(value = {"/auth"})
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserController userController;
    private final AuthService authService;

    private final Environment env;

    @Autowired
    public AuthController(UserService userService, JwtUtil jwtUtil, UserController userController, AuthService authService,
                          Environment env) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userController = userController;
        this.authService = authService;
        this.env = env;
    }

    @GetMapping("/{email}")
    ResponseEntity<User> getInfo(@PathVariable(required = false) String email) {
        User user = userService.getUserByEmail(email != null ? email : "help@me.com");
        String token = jwtUtil.generateToken(user);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return new ResponseEntity<>(user, headers, HttpStatus.OK);
    }

    @PostMapping("/register")
    ResponseEntity<?> registerUser(@RequestBody User user) {
        return this.userController.createUser(user);
    }

    @PostMapping("/login")
    ResponseEntity<AuthData> authenticateUser(@RequestBody AuthDTO authDTO,
                                              @RequestHeader(X_USER_DEVICE_ID) String deviceId,
                                              HttpServletRequest request) {
        authDTO.setDeviceId(deviceId);
        AuthData authData = authService.login(authDTO);
        // Add access token to authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + authData.getAccessToken());
        // Add refresh token to cookie
        long expirationMillis = Long.parseLong(Objects.requireNonNull(env.getProperty("jwt.refresh-token.exp-in-millis")));
        String cookiePath = buildRefreshTokenCookiePath(request);
        ResponseCookie refreshTokenCookie = ResponseCookie.from(X_REFRESH_TOKEN_COOKIE, authData.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path(cookiePath)
                .maxAge(expirationMillis)
                .sameSite("None")
                .build();
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        return new ResponseEntity<>(authData, headers, HttpStatus.OK);
    }

    @GetMapping("/logout")
    ResponseEntity<?> logout(@CookieValue(name=X_REFRESH_TOKEN_COOKIE) String refreshToken,
                             HttpServletRequest request, HttpServletResponse response,
                             @RequestHeader(X_USER_DEVICE_ID) String deviceId) {
        // Parsing access token
        String accessToken = jwtUtil.resolveToken(request);
        authService.logout(response, accessToken, refreshToken, deviceId);
        if (response.containsHeader(HttpHeaders.AUTHORIZATION)) {
            response.setHeader(HttpHeaders.AUTHORIZATION, null);
        }
        String cookiePath = buildRefreshTokenCookiePath(request);
        ResponseCookie refreshTokenCookie = ResponseCookie.from(X_REFRESH_TOKEN_COOKIE, null)
                .httpOnly(true)
                .secure(true)
                .path(cookiePath)
                .maxAge(0)
                .sameSite("None")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .build();
    }

    @GetMapping("/refreshToken")
    public ResponseEntity<AuthData> getRefreshToken(@CookieValue(name=X_REFRESH_TOKEN_COOKIE) String refreshToken,
                                    @RequestHeader(X_USER_DEVICE_ID) String deviceId, HttpServletRequest request) {
        AuthData authData = authService.getNewRefreshToken(refreshToken, deviceId);
        long expirationMillis = Long.parseLong(Objects.requireNonNull(env.getProperty("jwt.refresh-token.exp-in-millis")));
        String cookiePath = buildRefreshTokenCookiePath(request);
        ResponseCookie refreshTokenCookie = ResponseCookie.from(X_REFRESH_TOKEN_COOKIE, authData.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path(cookiePath)
                .maxAge(expirationMillis)
                .sameSite("None")
                .build();
        HttpHeaders headers = new HttpHeaders();
        // Add accessToken to authorization header
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + authData.getAccessToken());
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        // Disable caching authentication-related responses
        headers.setCacheControl(CacheControl.noCache().cachePrivate().mustRevalidate().getHeaderValue());
        headers.setPragma("no-cache");
        headers.setExpires(0);
        return new ResponseEntity<>(authData, headers, HttpStatus.OK);
    }

    @PostMapping("/network-status/{status}")
    public ResponseEntity<?> updateNetworkStatus(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestHeader(X_USER_DEVICE_ID) String deviceId, @PathVariable short status) {
        this.authService.updateNetworkStatus((User)userDetails, deviceId, status);
        return ResponseEntity.noContent().build();
    }

    private String buildRefreshTokenCookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        // Build the specific path for refresh token endpoint
        // You can choose one of these options:
        // Option A: Exact endpoint path (most secure)
        return request.getContextPath() + "/auth";
    }
}
