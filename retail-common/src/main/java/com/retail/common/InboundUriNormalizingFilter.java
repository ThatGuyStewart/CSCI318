package com.retail.common;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

final class InboundUriNormalizingFilter extends OncePerRequestFilter {

    private static final Pattern REPEATED_SLASHES = Pattern.compile("/{2,}");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String normalizedPath = normalize(request.getRequestURI());
        if (normalizedPath.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return normalizedPath;
            }

            @Override
            public String getServletPath() {
                return normalize(request.getServletPath());
            }

            @Override
            public String getPathInfo() {
                String pathInfo = request.getPathInfo();
                return pathInfo == null ? null : normalize(pathInfo);
            }

            @Override
            public StringBuffer getRequestURL() {
                StringBuffer requestUrl = request.getRequestURL();
                return new StringBuffer(requestUrl.substring(0, requestUrl.length() - request.getRequestURI().length()))
                        .append(normalizedPath);
            }
        }, response);
    }

    private String normalize(String path) {
        String normalized = REPEATED_SLASHES.matcher(path).replaceAll("/");
        return normalized.length() > 1 && normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }
}