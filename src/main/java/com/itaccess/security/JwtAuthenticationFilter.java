package com.itaccess.security;

import com.itaccess.entity.AuthLog;
import com.itaccess.repository.AuthLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserPrincipalService userPrincipalService;
    private final AuthLogRepository authLogRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return "OPTIONS".equalsIgnoreCase(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);
        String username = null;
        String errorReason = null;
        boolean success = false;

        log.info("JwtAuthenticationFilter executed for {} Authorization={}", request.getRequestURI(), StringUtils.hasText(jwt) ? "present" : "missing");

        if (StringUtils.hasText(jwt)) {
            try {
                username = tokenProvider.getUsernameFromToken(jwt);
            } catch (Exception ex) {
                errorReason = "INVALID_TOKEN";
            }

            if (errorReason == null) {
                try {
                    if (tokenProvider.validateToken(jwt)) {
                        Long userId = tokenProvider.getUserIdFromToken(jwt);
                        UserDetails userDetails = userPrincipalService.loadUserById(userId);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        success = true;
                    } else {
                        errorReason = "INVALID_TOKEN";
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                    errorReason = "TOKEN_EXPIRED";
                } catch (Exception ex) {
                    errorReason = "AUTH_SETUP_FAILED";
                    log.error("Impossible de definir l'authentification de l'utilisateur", ex);
                }
            }

            saveAuthLog(request, username, success, errorReason);
        }

        filterChain.doFilter(request, response);
    }

    private void saveAuthLog(HttpServletRequest request, String username, boolean success, String errorReason) {
        try {
            AuthLog logEntry = AuthLog.builder()
                    .username(username)
                    .success(success)
                    .errorReason(errorReason)
                    .ipAddress(getClientIP(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .createdAt(LocalDateTime.now())
                    .build();
            authLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Impossible de sauvegarder le log d'authentification", ex);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}