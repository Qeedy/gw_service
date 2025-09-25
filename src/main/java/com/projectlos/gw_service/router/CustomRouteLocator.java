package com.projectlos.gw_service.router;

import com.projectlos.gw_service.entity.RouteConfig;
import com.projectlos.gw_service.repository.RouteConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CustomRouteLocator {
    private final RouteConfigRepository routeConfigRepository;
    
    @Bean
    public RouteLocator dynamicRoutes(RouteLocatorBuilder builder) {
        List<RouteConfig> routeConfigs = routeConfigRepository.findByIsActiveTrueOrderByPriorityDesc();
        log.info("Route from DB: {}", routeConfigs);
        log.info("Building static routes for services...");
        
        RouteLocatorBuilder.Builder routes = builder.routes();
        // Add dynamic routes from database
        routeConfigs.forEach(rc -> {
            if (rc.getIsActive()) {
                log.info("Adding dynamic route: {} -> {} ({})", rc.getPathPattern(), rc.getUri(), rc.getRouteId());
                routes.route(rc.getRouteId(), r -> {
                    var route = r.path(rc.getPathPattern());
                    
                    // Add method filters if specified
                    if (rc.getPathPattern().contains("POST")) {
                        route.and().method(HttpMethod.POST);
                    } else if (rc.getPathPattern().contains("GET")) {
                        route.and().method(HttpMethod.GET);
                    } else if (rc.getPathPattern().contains("PUT")) {
                        route.and().method(HttpMethod.PUT);
                    } else if (rc.getPathPattern().contains("DELETE")) {
                        route.and().method(HttpMethod.DELETE);
                    }
                    
                    return route.uri(rc.getUri());
                });
            }
        });

//        // Static routes for core services
//        routes
//            // Customer Service
//            .route("customer-service", r -> r
//                .path("/api/customers/**")
//                .uri("http://localhost:8081"))
//
//            // Loan Service - Strip /api/loan prefix before forwarding
//            .route("loan-service", r -> {
//                log.info("Configuring loan-service route: /api/loan/** -> http://localhost:8082 with stripPrefix(2)");
//                return r.path("/api/loan/**")
//                    .filters(f -> f.stripPrefix(2))
//                    .uri("http://localhost:8082");
//            })
//
//            // Document Service
//            .route("document-service", r -> r
//                .path("/api/documents/**")
//                .uri("http://localhost:8083"));
        
        return routes.build();
    }
}
