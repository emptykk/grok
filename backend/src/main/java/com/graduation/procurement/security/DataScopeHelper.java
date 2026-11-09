package com.graduation.procurement.security;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.entity.SysRole;
import com.graduation.procurement.entity.SysUser;
import com.graduation.procurement.entity.SysUserRole;
import com.graduation.procurement.mapper.SysRoleMapper;
import com.graduation.procurement.mapper.SysUserMapper;
import com.graduation.procurement.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataScopeHelper {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public String currentScope() {
        LoginUser lu = SecurityUtils.currentUser();
        return scopeOf(lu.getId(), lu.getRoles());
    }

    public String scopeOf(Long userId, List<String> roleCodes) {
        if (roleCodes != null && roleCodes.contains("ADMIN")) return "ALL";
        List<SysUserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        String best = "SELF";
        for (SysUserRole ur : urs) {
            SysRole r = roleMapper.selectById(ur.getRoleId());
            if (r == null || r.getDataScope() == null) continue;
            if ("ALL".equals(r.getDataScope())) return "ALL";
            if ("DEPT".equals(r.getDataScope())) best = "DEPT";
        }
        return best;
    }

    public static String label(String scope) {
        if ("ALL".equals(scope)) return "全部数据";
        if ("DEPT".equals(scope)) return "本部门";
        return "仅本人";
    }

    public String currentDept() {
        SysUser u = userMapper.selectById(SecurityUtils.currentUser().getId());
        return u == null ? null : u.getDeptName();
    }
}
