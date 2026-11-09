package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.JobRunLog;
import com.graduation.procurement.entity.SysOperLog;
import com.graduation.procurement.mapper.JobRunLogMapper;
import com.graduation.procurement.mapper.SysOperLogMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OpsService {
    private final DataSource dataSource;
    private final JobRunLogMapper jobRunLogMapper;
    private final SysOperLogMapper operLogMapper;
    private final AiConfigService aiConfigService;

    public Map<String, Object> overview() {
        if (!SecurityUtils.hasRole("ADMIN")) throw new BizException("仅管理员可查看运行中心");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("health", health());
        m.put("ai", aiConfigService.getPublicConfig());
        m.put("jobs", recentJobs(20));
        m.put("jobCatalog", catalog());
        m.put("audit", audit());
        return m;
    }

    public Map<String, Object> health() {
        Map<String, Object> h = new LinkedHashMap<>();
        String db = "DOWN";
        String product = "";
        try (Connection c = dataSource.getConnection()) {
            db = c.isValid(2) ? "UP" : "DOWN";
            product = c.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            db = "DOWN";
            product = e.getMessage();
        }
        h.put("status", "UP".equals(db) ? "UP" : "DEGRADED");
        h.put("db", db);
        h.put("dbProduct", product);
        h.put("profile", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "h2"));
        h.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        h.put("java", System.getProperty("java.version"));
        h.put("now", LocalDateTime.now().toString().replace('T', ' ').substring(0, 19));
        return h;
    }

    public List<JobRunLog> recentJobs(int limit) {
        return jobRunLogMapper.selectList(new LambdaQueryWrapper<JobRunLog>()
                .orderByDesc(JobRunLog::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    private List<Map<String, Object>> catalog() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(job("inventory-alert", "库存预警扫描", "每天 08:00", "POST /api/jobs/inventory-alert"));
        list.add(job("contract-expiry", "合同到期扫描", "每天 08:30", "POST /api/jobs/contract-expiry"));
        return list;
    }

    private Map<String, Object> job(String code, String name, String cron, String endpoint) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("cron", cron);
        m.put("endpoint", endpoint);
        JobRunLog last = jobRunLogMapper.selectOne(new LambdaQueryWrapper<JobRunLog>()
                .eq(JobRunLog::getJobCode, code)
                .orderByDesc(JobRunLog::getCreatedAt)
                .last("LIMIT 1"));
        if (last != null) {
            m.put("lastStatus", last.getStatus());
            m.put("lastSummary", last.getSummary());
            m.put("lastAt", last.getCreatedAt());
            m.put("lastTrigger", last.getTriggerType());
        }
        return m;
    }

    private Map<String, Object> audit() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        long today = operLogMapper.selectCount(new LambdaQueryWrapper<SysOperLog>().ge(SysOperLog::getCreatedAt, start));
        long fail = operLogMapper.selectCount(new LambdaQueryWrapper<SysOperLog>().eq(SysOperLog::getSuccess, 0));
        List<SysOperLog> recent = operLogMapper.selectList(new LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getCreatedAt).last("LIMIT 8"));
        Map<String, Long> byModule = new LinkedHashMap<>();
        for (SysOperLog log : operLogMapper.selectList(new LambdaQueryWrapper<SysOperLog>().orderByDesc(SysOperLog::getId).last("LIMIT 200"))) {
            String key = log.getModule() == null ? "其他" : log.getModule();
            byModule.merge(key, 1L, Long::sum);
        }
        List<Map<String, Object>> modules = new ArrayList<>();
        byModule.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .forEach(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("module", e.getKey());
                    row.put("count", e.getValue());
                    modules.add(row);
                });
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("todayCount", today);
        a.put("failCount", fail);
        a.put("recent", recent);
        a.put("byModule", modules);
        return a;
    }
}
