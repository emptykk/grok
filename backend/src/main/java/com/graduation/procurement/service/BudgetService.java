package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.BudgetSubject;
import com.graduation.procurement.mapper.BudgetSubjectMapper;
import com.graduation.procurement.security.DataScopeHelper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetSubjectMapper mapper;
    private final OperLogService operLogService;
    private final DataScopeHelper dataScopeHelper;

    public Page<BudgetSubject> page(int page, int size, Integer year, String deptName, String keyword) {
        LambdaQueryWrapper<BudgetSubject> q = new LambdaQueryWrapper<>();
        if (year != null) q.eq(BudgetSubject::getBudgetYear, year);
        if (deptName != null && !deptName.isBlank()) q.eq(BudgetSubject::getDeptName, deptName);
        if (keyword != null && !keyword.isBlank()) q.and(w -> w.like(BudgetSubject::getName, keyword).or().like(BudgetSubject::getCode, keyword));
        q.orderByDesc(BudgetSubject::getBudgetYear).orderByAsc(BudgetSubject::getCode);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    public List<BudgetSubject> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<BudgetSubject>().orderByDesc(BudgetSubject::getBudgetYear).orderByAsc(BudgetSubject::getCode));
    }

    public BudgetSubject get(Long id) {
        BudgetSubject b = mapper.selectById(id);
        if (b == null) throw new BizException("预算科目不存在");
        return b;
    }

    public List<BudgetSubject> available() {
        LambdaQueryWrapper<BudgetSubject> q = new LambdaQueryWrapper<BudgetSubject>().eq(BudgetSubject::getStatus, 1);
        String scope = dataScopeHelper.currentScope();
        if ("SELF".equals(scope) || "DEPT".equals(scope)) {
            String dept = dataScopeHelper.currentDept();
            if (dept != null) q.eq(BudgetSubject::getDeptName, dept);
        }
        return mapper.selectList(q);
    }

    @Transactional
    public Long save(BudgetSubject body) {
        if (!SecurityUtils.hasAnyRole("ADMIN", "FINANCE", "PURCHASE_MANAGER")) throw new BizException("无权限维护预算");
        body.setUpdatedAt(LocalDateTime.now());
        if (body.getUsedAmount() == null) body.setUsedAmount(BigDecimal.ZERO);
        if (body.getStatus() == null) body.setStatus(1);
        if (body.getId() == null) {
            body.setCreatedAt(LocalDateTime.now());
            mapper.insert(body);
        } else {
            BudgetSubject old = get(body.getId());
            body.setUsedAmount(old.getUsedAmount());
            mapper.updateById(body);
        }
        operLogService.log("预算", body.getId() == null ? "新建" : "更新", body.getCode());
        return body.getId();
    }

    @Transactional
    public void delete(Long id) {
        BudgetSubject b = get(id);
        if (b.getUsedAmount() != null && b.getUsedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("已有占用金额，不能删除");
        }
        mapper.deleteById(id);
        operLogService.log("预算", "删除", b.getCode());
    }

    @Transactional
    public void occupy(Long subjectId, BigDecimal amount) {
        if (subjectId == null) return;
        BudgetSubject b = get(subjectId);
        BigDecimal used = b.getUsedAmount() == null ? BigDecimal.ZERO : b.getUsedAmount();
        BigDecimal total = b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount();
        if (used.add(amount).compareTo(total) > 0) {
            throw new BizException("超预算：科目「" + b.getName() + "」剩余 "
                    + total.subtract(used) + "，本次申请 " + amount);
        }
        b.setUsedAmount(used.add(amount));
        b.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(b);
    }

    @Transactional
    public void release(Long subjectId, BigDecimal amount) {
        if (subjectId == null || amount == null) return;
        BudgetSubject b = mapper.selectById(subjectId);
        if (b == null) return;
        BigDecimal used = b.getUsedAmount() == null ? BigDecimal.ZERO : b.getUsedAmount();
        BigDecimal next = used.subtract(amount);
        if (next.compareTo(BigDecimal.ZERO) < 0) next = BigDecimal.ZERO;
        b.setUsedAmount(next);
        b.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(b);
    }
}
