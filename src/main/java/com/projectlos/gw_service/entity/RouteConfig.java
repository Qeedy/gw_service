package com.projectlos.gw_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "route_id", length = 255, unique = true)
    private String routeId;
    
    @Column(name = "path_pattern", length = 255)
    private String pathPattern;
    
    @Column(name = "uri", length = 255)
    private String uri;
    
    @Column(name = "service_name", length = 100)
    private String serviceName;
    
    @Column(name = "requires_auth", nullable = false)
    private Boolean requiresAuth = true;
    
    @Column(name = "roles", length = 500)
    private String roles; // Comma-separated roles
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;
}

