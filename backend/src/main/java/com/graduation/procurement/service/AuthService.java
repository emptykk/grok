package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.SysRole;
import com.graduation.procurement.entity.SysUser;
import com.graduation.procurement.entity.SysUserRole;
import com.graduation.procurement.mapper.SysRoleMapper;
import com.graduation.procurement.mapper.SysUserMapper;
import com.graduation.procurement.mapper.SysUserRoleMapper;
import com.graduation.procurement.security.DataScopeHelper;
import com.graduation.procurement.security.JwtUtil;
import com.graduation.procurement.security.LoginUser;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OperLogService operLogService;
    private final DataScopeHelper dataScopeHelper;

    public Map<String, Object> login(String username, String password) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已停用");
        }
        List<String> roles = loadRoles(user.getId());
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), roles);
        operLogService.log("认证", "登录", username);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", toUserInfo(user, roles));
        return data;
    }

    public Map<String, Object> me() {
        LoginUser lu = SecurityUtils.currentUser();
        SysUser user = userMapper.selectById(lu.getId());
        if (user == null) throw new BizException("用户不存在");
        return toUserInfo(user, loadRoles(user.getId()));
    }

    public void changePassword(String oldPwd, String newPwd) {
        LoginUser lu = SecurityUtils.currentUser();
        SysUser user = userMapper.selectById(lu.getId());
        if (!passwordEncoder.matches(oldPwd, user.getPasswordHash())) {
            throw new BizException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        operLogService.log("认证", "改密", null);
    }

    public List<String> loadRoles(Long userId) {
        List<SysUserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (urs.isEmpty()) return List.of();
        List<Long> roleIds = urs.stream().map(SysUserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getCode).collect(Collectors.toList());
    }

    private Map<String, Object> toUserInfo(SysUser user, List<String> roles) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("deptName", user.getDeptName());
        m.put("phone", user.getPhone());
        m.put("roles", roles);
        String scope = dataScopeHelper.scopeOf(user.getId(), roles);
        m.put("dataScope", scope);
        m.put("dataScopeLabel", DataScopeHelper.label(scope));
        return m;
    }
}
