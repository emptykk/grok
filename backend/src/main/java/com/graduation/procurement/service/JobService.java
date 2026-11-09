package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {
    private final InventoryMapper inventoryMapper;
    private final MaterialMapper materialMapper;
    private final ContractInfoMapper contractMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final NotifyService notifyService;
    private final OperLogService operLogService;
    private final JobRunLogMapper jobRunLogMapper;

    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledInventoryAlert() { runInventoryAlert("SCHEDULE"); }

    @Scheduled(cron = "0 30 8 * * ?")
    public void scheduledContractExpiry() { runContractExpiry("SCHEDULE"); }

    public int runInventoryAlert() { return runInventoryAlert("MANUAL"); }

    public int runContractExpiry() { return runContractExpiry("MANUAL"); }

    public int runInventoryAlert(String trigger) {
        long t0 = System.currentTimeMillis();
        try {
            int n = 0;
            for (Inventory inv : inventoryMapper.selectList(new LambdaQueryWrapper<>())) {
                Material m = materialMapper.selectById(inv.getMaterialId());
                if (m == null || m.getSafeStock() == null) continue;
                if (inv.getQuantity().compareTo(m.getSafeStock()) < 0) {
                    notifyRole("WAREHOUSE", "库存预警", m.getName() + " 当前库存 " + inv.getQuantity() + " 低于安全库存 " + m.getSafeStock(), "INVENTORY", inv.getId());
                    notifyRole("ADMIN", "库存预警", m.getName() + " 低于安全库存", "INVENTORY", inv.getId());
                    n++;
                }
            }
            operLogService.log("定时任务", "库存预警", "alerts=" + n);
            record("inventory-alert", "库存预警扫描", trigger, "SUCCESS", "发现 " + n + " 条低于安全库存", null, t0);
            return n;
        } catch (RuntimeException e) {
            record("inventory-alert", "库存预警扫描", trigger, "FAIL", e.getMessage(), null, t0);
            throw e;
        }
    }

    public int runContractExpiry(String trigger) {
        long t0 = System.currentTimeMillis();
        try {
            List<ContractInfo> list = contractMapper.selectList(new LambdaQueryWrapper<ContractInfo>()
                    .eq(ContractInfo::getStatus, "ACTIVE")
                    .le(ContractInfo::getEndDate, LocalDate.now().plusDays(30))
                    .ge(ContractInfo::getEndDate, LocalDate.now()));
            for (ContractInfo c : list) {
                notifyRole("PURCHASE_MANAGER", "合同即将到期", c.getContractNo() + " " + c.getTitle() + " 到期日 " + c.getEndDate(), "CONTRACT", c.getId());
                notifyRole("FINANCE", "合同即将到期", c.getContractNo() + " 到期日 " + c.getEndDate(), "CONTRACT", c.getId());
            }
            operLogService.log("定时任务", "合同到期扫描", "count=" + list.size());
            record("contract-expiry", "合同到期扫描", trigger, "SUCCESS", "30 天内到期 " + list.size() + " 份", null, t0);
            return list.size();
        } catch (RuntimeException e) {
            record("contract-expiry", "合同到期扫描", trigger, "FAIL", e.getMessage(), null, t0);
            throw e;
        }
    }

    private void record(String code, String name, String trigger, String status, String summary, String detail, long t0) {
        try {
            JobRunLog row = new JobRunLog();
            row.setJobCode(code);
            row.setJobName(name);
            row.setTriggerType(trigger == null ? "MANUAL" : trigger);
            row.setStatus(status);
            row.setSummary(summary);
            row.setDetail(detail);
            row.setOperatorName(operatorName());
            row.setDurationMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - t0));
            row.setCreatedAt(LocalDateTime.now());
            jobRunLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("job run log skipped: {}", e.getMessage());
        }
    }

    private String operatorName() {
        try {
            return SecurityUtils.currentUser().getUsername();
        } catch (Exception e) {
            return "scheduler";
        }
    }

    private void notifyRole(String roleCode, String title, String content, String bizType, Long bizId) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode));
        if (role == null) return;
        List<SysUserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, role.getId()));
        for (SysUserRole ur : urs) notifyService.notifyUser(ur.getUserId(), title, content, bizType, bizId);
    }
}
