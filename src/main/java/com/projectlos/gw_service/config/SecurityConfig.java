package com.projectlos.gw_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String ROLE = "ROLE_";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .cors(cors -> {})
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // Public endpoints
                        .pathMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                        
                        // Auth endpoints (require authentication)
                        .pathMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .pathMatchers(HttpMethod.GET, "/auth/user-info").authenticated()
                        
                        // All service routes - just pass through with token relay
                        .pathMatchers("/api/**").permitAll()
                        
//                        // Customer service endpoints
//                        .pathMatchers(HttpMethod.POST, "/api/customers").hasAnyRole("ADMIN", "MAKER")
//                        .pathMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyRole("ADMIN", "MAKER", "CHECKER")
//                        .pathMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
//
//                        // Loan service endpoints
//                        .pathMatchers(HttpMethod.POST, "/api/loans").hasAnyRole("ADMIN", "MAKER")
//                        .pathMatchers(HttpMethod.GET, "/api/loans/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.PUT, "/api/loans/**").hasAnyRole("ADMIN", "MAKER", "CHECKER")
//                        .pathMatchers(HttpMethod.DELETE, "/api/loans/**").hasRole("ADMIN")
//
//                        // Identity documents endpoints
//                        .pathMatchers(HttpMethod.POST, "/api/identity-documents").hasAnyRole("ADMIN", "MAKER")
//                        .pathMatchers(HttpMethod.GET, "/api/identity-documents/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.PUT, "/api/identity-documents/**").hasAnyRole("ADMIN", "MAKER", "CHECKER")
//                        .pathMatchers(HttpMethod.DELETE, "/api/identity-documents/**").hasRole("ADMIN")
//
//                        // Collaterals endpoints
//                        .pathMatchers(HttpMethod.POST, "/api/collaterals").hasAnyRole("ADMIN", "MAKER")
//                        .pathMatchers(HttpMethod.GET, "/api/collaterals/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.PUT, "/api/collaterals/**").hasAnyRole("ADMIN", "MAKER", "CHECKER")
//                        .pathMatchers(HttpMethod.DELETE, "/api/collaterals/**").hasRole("ADMIN")
//
//                        // Tasks endpoints
//                        .pathMatchers(HttpMethod.GET, "/tasks/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.POST, "/tasks/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.PUT, "/tasks/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//
//                        // Document service endpoints
//                        .pathMatchers(HttpMethod.POST, "/api/documents").hasAnyRole("ADMIN", "MAKER")
//                        .pathMatchers(HttpMethod.GET, "/api/documents/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.PUT, "/api/documents/**").hasAnyRole("ADMIN", "MAKER", "CHECKER")
//                        .pathMatchers(HttpMethod.DELETE, "/api/documents/**").hasRole("ADMIN")
//
//                        // Disbursement endpoints (now handled by loan service)
//                        .pathMatchers(HttpMethod.POST, "/api/disbursements").hasAnyRole("ADMIN", "APPROVER")
//                        .pathMatchers(HttpMethod.GET, "/api/disbursements/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.PUT, "/api/disbursements/**").hasAnyRole("ADMIN", "APPROVER")
//
//                        // Notification service endpoints
//                        .pathMatchers(HttpMethod.POST, "/api/notifications").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//                        .pathMatchers(HttpMethod.GET, "/api/notifications/**").hasAnyRole("ADMIN", "MAKER", "CHECKER", "APPROVER")
//
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
            if (realmAccess == null || realmAccess.isEmpty()) {
                return Flux.empty();
            }
            
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get(ROLES);
            if (roles == null || roles.isEmpty()) {
                return Flux.empty();
            }
            
            return Flux.fromIterable(roles.stream()
                    .map(role -> new SimpleGrantedAuthority(ROLE + role))
                    .collect(Collectors.toList()));
        });
        
        return jwtAuthenticationConverter;
    }
}
