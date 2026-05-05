package com.itaccess.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
@Slf4j
public class RateLimitConfig implements Filter {
    
    private static final int MAX_REQUESTS = 70;
    private static final long TIME_WINDOW_MS = 160000; // 1 minute
    
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        
        // Appliquer le rate limiting uniquement sur /auth/**
        if (path.startsWith("/auth/")) {
            String clientIp = getClientIp(httpRequest);
            RateLimitInfo info = rateLimitMap.computeIfAbsent(clientIp, k -> new RateLimitInfo());
            
            long currentTime = System.currentTimeMillis();
            
            // Reset si la fenêtre de temps est passée
            if (currentTime - info.timestamp > TIME_WINDOW_MS) {
                info.count.set(0);
                info.timestamp = currentTime;
            }
            
            // Vérifier la limite
            if (info.count.incrementAndGet() > MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Trop de requêtes. Réessayez dans 1 minute.\"}");
                return;
            }
            
            // Ajouter les headers de rate limit
            httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS - info.count.get()));
            httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(info.timestamp + TIME_WINDOW_MS));
        }
        
        chain.doFilter(request, response);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    private static class RateLimitInfo {
        AtomicInteger count = new AtomicInteger(0);
        long timestamp = System.currentTimeMillis();
    }
}
