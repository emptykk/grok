package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.common.RequestStatuses;
import com.graduation.procurement.dto.ApprovalDTO;
import com.graduation.procurement.entity.PurchaseApproval;
import com.graduation.procurement.entity.PurchaseRequest;
import com.graduation.procurement.entity.SysUser;
import com.graduation.procurement.mapper.PurchaseApprovalMapper;
import com.graduation.procurement.mapper.PurchaseRequestMapper;
import com.graduation.procurement.mapper.SysUserMapper;
import com.graduation.procurement.security.LoginUser;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    private static final List<String> ORDER = List.of(
            RequestStatuses.DRAFT, RequestStatuses.PENDING_L1, RequestStatuses.PENDING_L2, RequestStatuses.PENDING_L3,
            RequestStatuses.PENDING_INQUIRY, RequestStatuses.PENDING_ORDER, RequestStatuses.PENDING_RECEIVE, RequestStatuses.COMPLETED);

    private final PurchaseRequestMapper requestMapper;
    private final PurchaseApprovalMapper approvalMapper;
    private final SysUserMapper userMapper;
    private final OperLogService operLogService;
    private final BudgetService budgetService;
    private final NotifyService notifyService;

    public List<PurchaseRequest> todo() {
        LambdaQueryWrapper<PurchaseRequest> q = new LambdaQueryWrapper<>();
        boolean l1 = SecurityUtils.hasAnyRole("DEPT_MANAGER", "ADMIN");
        boolean l2 = SecurityUtils.hasAnyRole("PURCHASE_MANAGER", "ADMIN");
        boolean l3 = SecurityUtils.hasAnyRole("FINANCE", "ADMIN");
        java.util.List<String> statuses = new java.util.ArrayList<>();
        if (l1) statuses.add(RequestStatuses.PENDING_L1);
        if (l2) statuses.add(RequestStatuses.PENDING_L2);
        if (l3) statuses.add(RequestStatuses.PENDING_L3);
        if (statuses.isEmpty()) return List.of();
        q.in(PurchaseRequest::getStatus, statuses);
        q.orderByAsc(PurchaseRequest::getCreatedAt);
        return requestMapper.selectList(q);
    }

    @Transactional
    public void approve(Long requestId, ApprovalDTO dto) {
        PurchaseRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("申请不存在");
        LoginUser lu = SecurityUtils.currentUser();
        int levels = req.getRequiredLevels() == null ? 2 : req.getRequiredLevels();
        String level;
        String nextStatus;
        if (RequestStatuses.PENDING_L1.equals(req.getStatus())) {
            if (!SecurityUtils.hasAnyRole("DEPT_MANAGER", "ADMIN")) throw new BizException("需要部门主管权限");
            level = "L1";
            if ("PASS".equalsIgnoreCase(dto.getResult())) {
                nextStatus = levels <= 1 ? RequestStatuses.PENDING_INQUIRY : RequestStatuses.PENDING_L2;
            } else nextStatus = RequestStatuses.REJECTED;
        } else if (RequestStatuses.PENDING_L2.equals(req.getStatus())) {
            if (!SecurityUtils.hasAnyRole("PURCHASE_MANAGER", "ADMIN")) throw new BizException("需要采购负责人权限");
            level = "L2";
            if ("PASS".equalsIgnoreCase(dto.getResult())) {
                nextStatus = levels <= 2 ? RequestStatuses.PENDING_INQUIRY : RequestStatuses.PENDING_L3;
            } else nextStatus = RequestStatuses.REJECTED;
        } else if (RequestStatuses.PENDING_L3.equals(req.getStatus())) {
            if (!SecurityUtils.hasAnyRole("FINANCE", "ADMIN")) throw new BizException("需要财务权限");
            level = "L3";
            nextStatus = "PASS".equalsIgnoreCase(dto.getResult()) ? RequestStatuses.PENDING_INQUIRY : RequestStatuses.REJECTED;
        } else {
            throw new BizException("当前状态不可审批");
        }
        PurchaseApproval ap = new PurchaseApproval();
        ap.setRequestId(requestId);
        ap.setLevel(level);
        ap.setApproverId(lu.getId());
        ap.setResult("PASS".equalsIgnoreCase(dto.getResult()) ? "PASS" : "REJECT");
        ap.setComment(dto.getComment());
        ap.setCreatedAt(LocalDateTime.now());
        approvalMapper.insert(ap);
        if (RequestStatuses.REJECTED.equals(nextStatus) && req.getBudgetSubjectId() != null) {
            budgetService.release(req.getBudgetSubjectId(), req.getBudgetAmount());
        }
        req.setStatus(nextStatus);
        req.setUpdatedAt(LocalDateTime.now());
        requestMapper.updateById(req);
        operLogService.log("审批", level + "-" + ap.getResult(), req.getRequestNo());
        notifyService.notifyUser(req.getApplicantId(), "采购申请审批更新",
                req.getRequestNo() + " " + level + " " + ap.getResult() + " => " + nextStatus, "REQUEST", req.getId());
    }

    public List<Map<String, Object>> timeline(Long requestId) {
        PurchaseRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new BizException("申请不存在");
        List<PurchaseApproval> aps = approvalMapper.selectList(new LambdaQueryWrapper<PurchaseApproval>()
                .eq(PurchaseApproval::getRequestId, requestId)
                .orderByAsc(PurchaseApproval::getCreatedAt));
        Map<String, PurchaseApproval> byLevel = new LinkedHashMap<>();
        for (PurchaseApproval a : aps) byLevel.put(a.getLevel(), a);
        int levels = req.getRequiredLevels() == null ? 2 : req.getRequiredLevels();
        String st = req.getStatus();
        boolean rejected = RequestStatuses.REJECTED.equals(st) || RequestStatuses.CANCELLED.equals(st);
        List<Map<String, Object>> items = new ArrayList<>();
        SysUser applicant = userMapper.selectById(req.getApplicantId());
        String applicantName = applicant == null ? "申请人" : applicant.getRealName();
        if (RequestStatuses.DRAFT.equals(st)) {
            items.add(node("提交申请", applicantName, req.getCreatedAt(), "active", "草稿，尚未提交"));
        } else {
            items.add(node("提交申请", applicantName, req.getCreatedAt(), "done", "已提交 " + req.getRequestNo()));
        }
        addLevel(items, byLevel, "L1", "一级审批 · 部门主管", RequestStatuses.PENDING_L1, st, rejected);
        if (levels >= 2) addLevel(items, byLevel, "L2", "二级审批 · 采购负责人", RequestStatuses.PENDING_L2, st, rejected);
        if (levels >= 3) addLevel(items, byLevel, "L3", "三级审批 · 财务", RequestStatuses.PENDING_L3, st, rejected);
        addStage(items, "询价比价", RequestStatuses.PENDING_INQUIRY, st, rejected, "采购负责人组织多轮报价");
        addStage(items, "生成订单", RequestStatuses.PENDING_ORDER, st, rejected, "选定供应商后生成采购订单");
        addStage(items, "入库验收", RequestStatuses.PENDING_RECEIVE, st, rejected, "仓管按单验收，可记录到货差异");
        if (RequestStatuses.COMPLETED.equals(st)) {
            items.add(node("完成", "系统", req.getUpdatedAt(), "done", "采购闭环已完成"));
        } else if (rejected) {
            items.add(node(RequestStatuses.CANCELLED.equals(st) ? "已取消" : "已驳回", "—", req.getUpdatedAt(), "reject", "流程终止"));
        } else {
            items.add(node("完成", "—", null, "pending", "尚未闭环"));
        }
        return items;
    }

    private void addLevel(List<Map<String, Object>> items, Map<String, PurchaseApproval> byLevel, String level, String title,
                          String pendingStatus, String current, boolean rejected) {
        PurchaseApproval a = byLevel.get(level);
        if (a != null) {
            SysUser u = userMapper.selectById(a.getApproverId());
            String name = u == null ? "审批人" : u.getRealName();
            boolean rej = "REJECT".equalsIgnoreCase(a.getResult());
            String comment = (rej ? "驳回" : "通过") + (a.getComment() == null || a.getComment().isBlank() ? "" : " · " + a.getComment());
            items.add(node(title, name, a.getCreatedAt(), rej ? "reject" : "done", comment));
            return;
        }
        if (pendingStatus.equals(current)) {
            items.add(node(title, "待处理", null, "active", "等待当前节点审批"));
        } else if (!rejected && indexOf(current) > indexOf(pendingStatus)) {
            items.add(node(title, "—", null, "done", "已通过（轨迹未单独落库）"));
        } else {
            items.add(node(title, "—", null, "pending", "未到达"));
        }
    }

    private void addStage(List<Map<String, Object>> items, String title, String stage, String current, boolean rejected, String hint) {
        if (rejected) {
            items.add(node(title, "—", null, "pending", "流程已终止"));
            return;
        }
        int cur = indexOf(current);
        int idx = indexOf(stage);
        if (cur == idx) items.add(node(title, "进行中", null, "active", hint));
        else if (cur > idx) items.add(node(title, "已完成", null, "done", hint));
        else items.add(node(title, "—", null, "pending", "未到达"));
    }

    private int indexOf(String status) {
        int i = ORDER.indexOf(status);
        return i < 0 ? -1 : i;
    }

    private Map<String, Object> node(String step, String actor, LocalDateTime time, String status, String comment) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("step", step);
        m.put("actor", actor);
        m.put("time", time == null ? "" : time.toString().replace('T', ' '));
        m.put("status", status);
        m.put("comment", comment);
        return m;
    }
}
