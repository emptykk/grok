package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MasterDataService {
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final InventoryMapper inventoryMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysOperLogMapper logMapper;
    private final OperLogService operLogService;

    public Page<Material> pageMaterials(int page, int size, String keyword) {
        LambdaQueryWrapper<Material> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) q.and(w -> w.like(Material::getName, keyword).or().like(Material::getCode, keyword));
        q.orderByDesc(Material::getId);
        return materialMapper.selectPage(new Page<>(page, size), q);
    }

    public void saveMaterial(Material m) {
        m.setUpdatedAt(LocalDateTime.now());
        if (m.getId() == null) {
            m.setCreatedAt(LocalDateTime.now());
            materialMapper.insert(m);
            Inventory inv = new Inventory();
            inv.setMaterialId(m.getId());
            inv.setQuantity(BigDecimal.ZERO);
            inv.setUpdatedAt(LocalDateTime.now());
            inventoryMapper.insert(inv);
        } else materialMapper.updateById(m);
        operLogService.log("物资", "保存", m.getCode());
    }

    public void deleteMaterial(Long id) {
        materialMapper.deleteById(id);
        inventoryMapper.delete(new LambdaQueryWrapper<Inventory>().eq(Inventory::getMaterialId, id));
    }

    public Page<Supplier> pageSuppliers(int page, int size, String keyword) {
        LambdaQueryWrapper<Supplier> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) q.like(Supplier::getName, keyword);
        q.orderByDesc(Supplier::getRating);
        return supplierMapper.selectPage(new Page<>(page, size), q);
    }

    public void saveSupplier(Supplier s) {
        s.setUpdatedAt(LocalDateTime.now());
        if (s.getBlacklisted() == null) s.setBlacklisted(0);
        if (s.getAdmissionScore() == null) s.setAdmissionScore(BigDecimal.ZERO);
        if (s.getId() == null) { s.setCreatedAt(LocalDateTime.now()); supplierMapper.insert(s); }
        else supplierMapper.updateById(s);
        operLogService.log("供应商", "保存", s.getName());
    }

    public void deleteSupplier(Long id) { supplierMapper.deleteById(id); }

    public List<Map<String, Object>> inventories() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Inventory inv : inventoryMapper.selectList(new LambdaQueryWrapper<>())) {
            Material m = materialMapper.selectById(inv.getMaterialId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", inv.getId());
            row.put("materialId", inv.getMaterialId());
            row.put("quantity", inv.getQuantity());
            row.put("material", m);
            row.put("alert", m != null && m.getSafeStock() != null && inv.getQuantity().compareTo(m.getSafeStock()) < 0);
            list.add(row);
        }
        return list;
    }

    public Page<SysUser> pageUsers(int page, int size) {
        return userMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
    }

    public List<SysRole> roles() { return roleMapper.selectList(new LambdaQueryWrapper<>()); }

    public void updateRoleDataScope(Long roleId, String dataScope) {
        if (!SecurityUtils.hasRole("ADMIN")) throw new BizException("仅管理员可改数据权限");
        if (!List.of("SELF","DEPT","ALL").contains(dataScope)) throw new BizException("dataScope 仅支持 SELF/DEPT/ALL");
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BizException("角色不存在");
        role.setDataScope(dataScope);
        roleMapper.updateById(role);
        operLogService.log("角色", "数据权限", role.getCode() + "=" + dataScope);
    }

    public Page<SysOperLog> logs(int page, int size, String username, String module, String action, String keyword, Integer success) {
        return logMapper.selectPage(new Page<>(page, size), logQuery(username, module, action, keyword, success));
    }

    public SysOperLog logDetail(Long id) {
        SysOperLog log = logMapper.selectById(id);
        if (log == null) throw new BizException("日志不存在");
        return log;
    }

    public List<SysOperLog> logsForExport(String username, String module, String action, String keyword, Integer success) {
        return logMapper.selectList(logQuery(username, module, action, keyword, success).last("LIMIT 5000"));
    }

    private LambdaQueryWrapper<SysOperLog> logQuery(String username, String module, String action, String keyword, Integer success) {
        LambdaQueryWrapper<SysOperLog> q = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) q.like(SysOperLog::getUsername, username.trim());
        if (module != null && !module.isBlank()) q.eq(SysOperLog::getModule, module.trim());
        if (action != null && !action.isBlank()) q.like(SysOperLog::getAction, action.trim());
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            q.and(w -> w.like(SysOperLog::getDetail, kw).or().like(SysOperLog::getRequestUri, kw).or().like(SysOperLog::getUsername, kw));
        }
        if (success != null) q.eq(SysOperLog::getSuccess, success);
        q.orderByDesc(SysOperLog::getCreatedAt);
        return q;
    }
}
