package com.graduation.procurement.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.procurement.common.Result;
import com.graduation.procurement.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final ObjectMapper om = new ObjectMapper();
    private static final int LIMIT = 120;
    private static final long WINDOW = 60_000L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = keyOf();
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW) q.pollFirst();
            if (q.size() >= LIMIT) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(om.writeValueAsString(Result.fail(429, "请求过于频繁，请稍后再试")));
                return false;
            }
            q.addLast(now);
        }
        return true;
    }

    private String keyOf() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu) return "u:" + lu.getId();
        return "anon";
    }
}
