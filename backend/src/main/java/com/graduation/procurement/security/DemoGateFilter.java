package com.graduation.procurement.security;

import com.graduation.procurement.config.PublicDeployProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class DemoGateFilter extends OncePerRequestFilter {
    public static final String COOKIE = "demo_gate";
    private static final String PAYLOAD = "demo-gate-v1";

    private final PublicDeployProperties props;

    public DemoGateFilter(PublicDeployProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isPublicMode()) return true;
        String uri = request.getRequestURI();
        return "/api/health".equals(uri)
                || "/api/auth/gate".equals(uri)
                || "/api/auth/bootstrap".equals(uri)
                || "/gate.html".equals(uri)
                || "/favicon.ico".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (cookieOk(request)) {
            chain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"需要先通过演示门禁\",\"data\":null}");
            return;
        }
        response.sendRedirect("/gate.html");
    }

    public boolean cookieOk(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        String expected = token();
        for (Cookie c : cookies) {
            if (COOKIE.equals(c.getName()) && constantEquals(expected, c.getValue())) return true;
        }
        return false;
    }

    public String token() {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.getDemoGateSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(PAYLOAD.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("gate token unavailable");
        }
    }

    public static boolean constantEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
