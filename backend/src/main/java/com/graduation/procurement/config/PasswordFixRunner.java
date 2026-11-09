package com.graduation.procurement.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.entity.SysUser;
import com.graduation.procurement.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@RequiredArgsConstructor
public class PasswordFixRunner implements CommandLineRunner {
    private final SysUserMapper userMapper;
    private final PasswordEncoder encoder;
    private final PublicDeployProperties deploy;

    @Override
    public void run(String... args) {
        String password = deploy.getDemoPassword();
        if (password == null || password.isBlank()) {
            if (deploy.isPublicMode()) return;
            password = "123456";
        }
        for (SysUser u : userMapper.selectList(new LambdaQueryWrapper<>())) {
            if (!encoder.matches(password, u.getPasswordHash())) {
                u.setPasswordHash(encoder.encode(password));
                userMapper.updateById(u);
            }
        }
    }
}
