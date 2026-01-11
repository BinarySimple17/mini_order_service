package ru.binarysimple.order.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

//@RequiredArgsConstructor
@Component
public class UserAuthFilter extends OncePerRequestFilter {

//    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // будь у нас JWT авторизация, то тут можно было бы верифицировать токен

        String userIdHeader = request.getHeader("X-Username");

        logger.debug("Filter X-Username header: " + userIdHeader);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userIdHeader, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
//                auth.setAuthenticated(true); // ← явно помечаем как аутентифицированного
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid X-Username header: " + userIdHeader);
            }
        }
        filterChain.doFilter(request, response);
    }
}
