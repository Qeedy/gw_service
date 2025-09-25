package com.projectlos.gw_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectlos.gw_service.config.KeycloakProperties;
import com.projectlos.gw_service.model.request.LoginRequest;
import com.projectlos.gw_service.model.request.RefreshTokenRequest;
import com.projectlos.gw_service.model.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final WebClient webClient;
    private final KeycloakProperties keycloakProperties;
    private final ObjectMapper objectMapper;

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        return webClient.post()
                .uri(keycloakProperties.getTokenUri())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(USERNAME, request.getUsername())
                        .with(PASSWORD, request.getPassword())
                        .with(GRANT_TYPE, "password")
                        .with(CLIENT_ID, keycloakProperties.getClientId())
                        .with(CLIENT_SECRET, keycloakProperties.getClientSecret()))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseLoginResponse)
                .map(loginResponse -> {
                    loginResponse.setUsername(request.getUsername());
                    loginResponse.setLoginTime(LocalDateTime.now());
                    loginResponse.setMessage("Login successful");
                    return ResponseEntity.ok().body(loginResponse);
                })
                .onErrorResume(throwable -> {
                    log.error("Login failed for user: {}", request.getUsername(), throwable);
                    LoginResponse errorResponse = LoginResponse.builder()
                            .message("Login failed: " + throwable.getMessage())
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        
        return webClient.post()
                .uri(keycloakProperties.getTokenUri())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(GRANT_TYPE, "refresh_token")
                        .with(REFRESH_TOKEN, request.getRefreshToken())
                        .with(CLIENT_ID, keycloakProperties.getClientId())
                        .with(CLIENT_SECRET, keycloakProperties.getClientSecret()))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseLoginResponse)
                .map(loginResponse -> {
                    loginResponse.setLoginTime(LocalDateTime.now());
                    loginResponse.setMessage("Token refreshed successfully");
                    return ResponseEntity.ok().body(loginResponse);
                })
                .onErrorResume(throwable -> {
                    log.error("Token refresh failed", throwable);
                    LoginResponse errorResponse = LoginResponse.builder()
                            .message("Token refresh failed: " + throwable.getMessage())
                            .build();
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<String>> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            log.info("Logout attempt for user: {}", username);
        }
        
        return Mono.just(ResponseEntity.ok().body("{\"message\": \"Logout successful\"}"));
    }

    @GetMapping("/user-info")
    public Mono<ResponseEntity<String>> getUserInfo(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");
            
            String userInfo = String.format(
                "{\"username\":\"%s\",\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"roles\":%s}",
                username, email, firstName, lastName, authentication.getAuthorities().toString()
            );
            
            return Mono.just(ResponseEntity.ok().body(userInfo));
        }
        
        return Mono.just(ResponseEntity.badRequest().body("{\"error\": \"User not authenticated\"}"));
    }

    private LoginResponse parseLoginResponse(String jsonResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            return LoginResponse.builder()
                    .accessToken(jsonNode.get("access_token").asText())
                    .tokenType(jsonNode.get("token_type").asText())
                    .refreshToken(jsonNode.get("refresh_token").asText())
                    .expiresIn(jsonNode.get("expires_in").asLong())
                    .scope(jsonNode.get("scope").asText())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse login response", e);
            throw new RuntimeException("Failed to parse login response", e);
        }
    }
}