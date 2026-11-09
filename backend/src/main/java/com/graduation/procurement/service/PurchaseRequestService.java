package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.common.RequestStatuses;
import com.graduation.procurement.dto.RequestSaveDTO;
import com.graduation.procurement.entity.*;
import com.graduation.procurement.mapper.*;
import com.graduation.procurement.security.DataScopeHelper;
import com.graduation.procurement.security.LoginUser;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseRequestService {
    private final PurchaseRequestMapper requestMapper;
    private final PurchaseRequestItemMapper itemMapper;
    private final PurchaseApprovalMapper approvalMapper;
    private final SysUserMapper userMapper;
    private final OperLogService operLogService;
    private final BudgetService budgetService;
    private final ApprovalRuleService approvalRuleService;
    private final DataScopeHelper dataScopeHelper;
    private final NotifyService notifyService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public Page<PurchaseRequest> page(int page, int size, String status, String keyword, Long budgetSubjectId) {
        LambdaQueryWrapper<PurchaseRequest> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq(PurchaseRequest::getStatus, status);
        if (budgetSubjectId != null) q.eq(PurchaseRequest::getBudgetSubjectId, budgetSubjectId);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(PurchaseRequest::getTitle, keyword).or().like(PurchaseRequest::getRequestNo, keyword));
        }
        String scope = dataScopeHelper.currentScope();
        LoginUser lu = SecurityUtils.currentUser();
        if ("SELF".equals(scope)) {
            q.eq(PurchaseRequest::getApplicantId, lu.getId());
        } else if ("DEPT".equals(scope)) {
            String dept = dataScopeHelper.currentDept();
            if (dept != null) q.eq(PurchaseRequest::getDeptName, dept);
            else q.eq(PurchaseRequest::getApplicantId, lu.getId());
        }
        q.orderByDesc(PurchaseRequest::getCreatedAt);
        return requestMapper.selectPage(new Page<>(page, size), q);
    }

    public Map<String, Object> detail(Long id) {
        PurchaseRequest req = requestMapper.selectById(id);
        if (req == null) throw new BizException("申请不存在");
        List<PurchaseRequestItem> items = itemMapper.selectList(new LambdaQueryWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, id));
        List<PurchaseApproval> approvals = approvalMapper.selectList(new LambdaQueryWrapper<PurchaseApproval>().eq(PurchaseApproval::getRequestId, id).orderByAsc(PurchaseApproval::getCreatedAt));
        SysUser applicant = userMapper.selectById(req.getApplicantId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("request", req);
        m.put("items", items);
        m.put("approvals", approvals);
        m.put("applicantName", applicant == null ? null : applicant.getRealName());
        if (req.getBudgetSubjectId() != null) {
            try { m.put("budgetSubject", budgetService.get(req.getBudgetSubjectId())); } catch (Exception ignored) {}
        }
        return m;
    }

    @Transactional
    public Long save(RequestSaveDTO dto) {
        LoginUser lu = SecurityUtils.currentUser();
        PurchaseRequest req;
        if (dto.getId() != null) {
            req = requestMapper.selectById(dto.getId());
            if (req == null) throw new BizException("申请不存在");
            if (!RequestStatuses.DRAFT.equals(req.getStatus()) && !RequestStatuses.REJECTED.equals(req.getStatus())) {
                throw new BizException("仅草稿或已驳回可编辑");
            }
            if (!req.getApplicantId().equals(lu.getId()) && !SecurityUtils.hasRole("ADMIN")) {
                throw new BizException("只能编辑自己的申请");
            }
        } else {
            req = new PurchaseRequest();
            req.setRequestNo(genNo("PR"));
            req.setApplicantId(lu.getId());
            SysUser u = userMapper.selectById(lu.getId());
            req.setDeptName(u == null ? null : u.getDeptName());
            req.setCreatedAt(LocalDateTime.now());
        }
        req.setTitle(dto.getTitle());
        req.setBudgetAmount(dto.getBudgetAmount() == null ? BigDecimal.ZERO : dto.getBudgetAmount());
        req.setBudgetSubjectId(dto.getBudgetSubjectId());
        req.setRemark(dto.getRemark());
        req.setAiPrompt(dto.getAiPrompt());
        req.setStatus(RequestStatuses.DRAFT);
        req.setUpdatedAt(LocalDateTime.now());
        if (req.getId() == null) requestMapper.insert(req); else requestMapper.updateById(req);

        itemMapper.delete(new LambdaQueryWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, req.getId()));
        BigDecimal budget = BigDecimal.ZERO;
        if (dto.getItems() != null) {
            for (RequestSaveDTO.Item it : dto.getItems()) {
                PurchaseRequestItem item = new PurchaseRequestItem();
                item.setRequestId(req.getId());
                item.setMaterialId(it.getMaterialId());
                item.setMaterialName(it.getMaterialName());
                item.setSpec(it.getSpec());
                item.setUnit(it.getUnit());
                item.setQuantity(it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity());
                item.setEstimatePrice(it.getEstimatePrice() == null ? BigDecimal.ZERO : it.getEstimatePrice());
                item.setAmount(item.getQuantity().multiply(item.getEstimatePrice()));
                item.setRemark(it.getRemark());
                itemMapper.insert(item);
                budget = budget.add(item.getAmount());
            }
        }
        if (dto.getBudgetAmount() == null) {
            req.setBudgetAmount(budget);
            requestMapper.updateById(req);
        }
        operLogService.log("采购申请", "保存", req.getRequestNo());
        return req.getId();
    }

    @Transactional
    public void submit(Long id) {
        PurchaseRequest req = requestMapper.selectById(id);
        if (req == null) throw new BizException("申请不存在");
        LoginUser lu = SecurityUtils.currentUser();
        if (!req.getApplicantId().equals(lu.getId()) && !SecurityUtils.hasRole("ADMIN")) {
            throw new BizException("只能提交自己的申请");
        }
        if (!RequestStatuses.DRAFT.equals(req.getStatus()) && !RequestStatuses.REJECTED.equals(req.getStatus())) {
            throw new BizException("当前状态不可提交");
        }
        long cnt = itemMapper.selectCount(new LambdaQueryWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, id));
        if (cnt == 0) throw new BizException("请先添加明细");
        BigDecimal amount = req.getBudgetAmount() == null ? BigDecimal.ZERO : req.getBudgetAmount();
        int levels = approvalRuleService.requiredLevels(amount);
        req.setRequiredLevels(levels);
        // 超预算拦截 + 占用
        if (req.getBudgetSubjectId() != null) {
            budgetService.occupy(req.getBudgetSubjectId(), amount);
        }
        req.setStatus(RequestStatuses.PENDING_L1);
        req.setUpdatedAt(LocalDateTime.now());
        requestMapper.updateById(req);
        operLogService.log("采购申请", "提交", req.getRequestNo() + " levels=" + levels);
        notifyRoleUsers("DEPT_MANAGER", "待一级审批", "申请 " + req.getRequestNo() + " 待您审批", "REQUEST", req.getId());
    }

    private void notifyRoleUsers(String roleCode, String title, String content, String bizType, Long bizId) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode));
        if (role == null) return;
        List<SysUserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, role.getId()));
        for (SysUserRole ur : urs) notifyService.notifyUser(ur.getUserId(), title, content, bizType, bizId);
    }

    @Transactional
    public void withdraw(Long id) {
        PurchaseRequest req = requestMapper.selectById(id);
        if (req == null) throw new BizException("申请不存在");
        LoginUser lu = SecurityUtils.currentUser();
        if (!req.getApplicantId().equals(lu.getId()) && !SecurityUtils.hasRole("ADMIN")) {
            throw new BizException("只能撤回自己的申请");
        }
        if (!RequestStatuses.PENDING_L1.equals(req.getStatus()) && !RequestStatuses.PENDING_L2.equals(req.getStatus())
                && !RequestStatuses.PENDING_L3.equals(req.getStatus())) {
            throw new BizException("仅待审批状态可撤回");
        }
        if (req.getBudgetSubjectId() != null) budgetService.release(req.getBudgetSubjectId(), req.getBudgetAmount());
        req.setStatus(RequestStatuses.CANCELLED);
        req.setUpdatedAt(LocalDateTime.now());
        requestMapper.updateById(req);
        operLogService.log("采购申请", "撤回", req.getRequestNo());
    }

    @Transactional
    public void delete(Long id) {
        PurchaseRequest req = requestMapper.selectById(id);
        if (req == null) return;
        if (!RequestStatuses.DRAFT.equals(req.getStatus())) throw new BizException("仅草稿可删除");
        itemMapper.delete(new LambdaQueryWrapper<PurchaseRequestItem>().eq(PurchaseRequestItem::getRequestId, id));
        requestMapper.deleteById(id);
        operLogService.log("采购申请", "删除", req.getRequestNo());
    }

    public List<PurchaseRequest> listForExport(String status) {
        LambdaQueryWrapper<PurchaseRequest> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq(PurchaseRequest::getStatus, status);
        q.orderByDesc(PurchaseRequest::getCreatedAt);
        return requestMapper.selectList(q);
    }

    public static String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int)(Math.random()*900+100);
    }
}
