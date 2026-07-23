package com.tradernet.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class SpaRoutingFilter implements Filter {

    private static final String INDEX_PATH = "/index.html";

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final String path = getApplicationPath(httpRequest);
        final boolean resourceExists = httpRequest.getServletContext().getResource(path) != null;

        if (shouldForward(httpRequest.getMethod(), path, resourceExists)) {
            httpRequest.getRequestDispatcher(INDEX_PATH).forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    static boolean shouldForward(String method, String path, boolean resourceExists) {
        if (!("GET".equals(method) || "HEAD".equals(method)) || resourceExists) {
            return false;
        }
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return false;
        }
        if (isPathOrDescendant(path, "/api")
            || isPathOrDescendant(path, "/assets")
            || isPathOrDescendant(path, "/WEB-INF")
            || isPathOrDescendant(path, "/META-INF")) {
            return false;
        }

        final int lastSlash = path.lastIndexOf('/');
        return path.indexOf('.', lastSlash + 1) < 0;
    }

    private static boolean isPathOrDescendant(String path, String excludedPath) {
        return path.equals(excludedPath) || path.startsWith(excludedPath + "/");
    }

    private static String getApplicationPath(HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        final String contextPath = request.getContextPath();
        final String path = requestUri.substring(contextPath.length());
        return path.isEmpty() ? "/" : path;
    }
}
