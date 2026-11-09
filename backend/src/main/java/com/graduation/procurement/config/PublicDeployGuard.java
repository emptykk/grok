package com.graduation.procurement.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class PublicDeployGuard implements ApplicationRunner {
    private final PublicDeployProperties props;

    public PublicDeployGuard(PublicDeployProperties props) {
        this.props = props;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (!props.isPublicMode()) return;
        if (props.getDemoGateSecret().isBlank() || props.getDemoGateSecret().length() < 16) {
            throw new IllegalStateException("PUBLIC_MODE requires DEMO_GATE_SECRET (at least 16 characters)");
        }
        if (props.getDemoPassword().isBlank() || props.getDemoPassword().length() < 10) {
            throw new IllegalStateException("PUBLIC_MODE requires DEMO_PASSWORD (at least 10 characters)");
        }
        if ("123456".equals(props.getDemoPassword())) {
            throw new IllegalStateException("DEMO_PASSWORD must not be the local demo password when PUBLIC_MODE is enabled");
        }
        String jwt = System.getenv("JWT_SECRET");
        if (jwt == null || jwt.isBlank() || jwt.length() < 32) {
            throw new IllegalStateException("PUBLIC_MODE requires JWT_SECRET (at least 32 characters)");
        }
    }
}
