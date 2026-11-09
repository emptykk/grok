package com.graduation.procurement.service;
import com.graduation.procurement.entity.SysOperLog;
import com.graduation.procurement.mapper.SysOperLogMapper;
import com.graduation.procurement.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperLogService {
    private final SysOperLogMapper logMapper;

    public void log(String module, String action, String detail) {
        log(module, action, detail, true, null);
    }

    public void log(String module, String action, String detail, boolean success, Long costMs) {
        SysOperLog log = new SysOperLog();
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setSuccess(success ? 1 : 0);
        log.setCostMs(costMs);
        log.setCreatedAt(LocalDateTime.now());
        try { log.setUsername(SecurityUtils.currentUser().getUsername()); }
        catch (Exception e) { log.setUsername("anonymous"); }
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                log.setIp(req.getRemoteAddr());
                log.setMethod(req.getMethod());
                log.setRequestUri(req.getRequestURI());
            }
        } catch (Exception ignored) {}
        logMapper.insert(log);
    }
}
