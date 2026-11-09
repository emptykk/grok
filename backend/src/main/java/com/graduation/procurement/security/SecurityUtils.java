package com.graduation.procurement.security;

import com.graduation.procurement.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static LoginUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser lu)) {
            throw new BizException(401, "未登录");
        }
        return lu;
    }

    public static boolean hasRole(String role) {
        return currentUser().getRoles().contains(role);
    }

    public static boolean hasAnyRole(String... roles) {
        for (String r : roles) if (hasRole(r)) return true;
        return false;
    }
}
