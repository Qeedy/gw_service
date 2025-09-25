package com.projectlos.gw_service.repository;

import com.projectlos.gw_service.entity.RouteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteConfigRepository extends JpaRepository<RouteConfig, Long> {
    
    List<RouteConfig> findByIsActiveTrueOrderByPriorityDesc();
    
    Optional<RouteConfig> findByRouteId(String routeId);
    
    List<RouteConfig> findByServiceName(String serviceName);
    
    List<RouteConfig> findByRequiresAuthTrue();
}
