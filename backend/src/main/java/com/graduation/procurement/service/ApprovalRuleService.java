package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.ApprovalRule;
import com.graduation.procurement.mapper.ApprovalRuleMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalRuleService {
    private final ApprovalRuleMapper mapper;
    private final OperLogService operLogService;

    public List<ApprovalRule> list() {
        return mapper.selectList(new LambdaQueryWrapper<ApprovalRule>().orderByAsc(ApprovalRule::getMinAmount));
    }

    public Map<String, Object> resolve(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        ApprovalRule hit = findRule(amount);
        Map<String, Object> m = new HashMap<>();
        m.put("amount", amount);
        m.put("levels", hit == null ? 2 : hit.getLevels());
        m.put("rule", hit);
        return m;
    }

    public ApprovalRule findRule(BigDecimal amount) {
        List<ApprovalRule> rules = mapper.selectList(new LambdaQueryWrapper<ApprovalRule>()
                .eq(ApprovalRule::getEnabled, 1).orderByAsc(ApprovalRule::getMinAmount));
        for (ApprovalRule r : rules) {
            if (amount.compareTo(r.getMinAmount()) >= 0 && amount.compareTo(r.getMaxAmount()) <= 0) return r;
        }
        return null;
    }

    public int requiredLevels(BigDecimal amount) {
        ApprovalRule r = findRule(amount == null ? BigDecimal.ZERO : amount);
        return r == null ? 2 : r.getLevels();
    }

    @Transactional
    public Long save(ApprovalRule body) {
        if (!SecurityUtils.hasRole("ADMIN")) throw new BizException("仅管理员可配置审批规则");
        if (body.getLevels() == null || body.getLevels() < 1 || body.getLevels() > 3) throw new BizException("levels 只能为 1/2/3");
        if (body.getId() == null) {
            body.setCreatedAt(LocalDateTime.now());
            if (body.getEnabled() == null) body.setEnabled(1);
            mapper.insert(body);
        } else {
            mapper.updateById(body);
        }
        operLogService.log("审批规则", "保存", body.getName());
        return body.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (!SecurityUtils.hasRole("ADMIN")) throw new BizException("仅管理员可配置审批规则");
        mapper.deleteById(id);
        operLogService.log("审批规则", "删除", String.valueOf(id));
    }
}
