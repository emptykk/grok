package com.graduation.procurement.service;
import com.graduation.procurement.entity.EmailSendLog;
import com.graduation.procurement.entity.SysMessage;
import com.graduation.procurement.entity.SysUser;
import com.graduation.procurement.mapper.EmailSendLogMapper;
import com.graduation.procurement.mapper.SysMessageMapper;
import com.graduation.procurement.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotifyService {
    private final SysMessageMapper messageMapper;
    private final EmailSendLogMapper emailSendLogMapper;
    private final SysUserMapper userMapper;

    public void notifyUser(Long userId, String title, String content, String bizType, Long bizId) {
        if (userId == null) return;
        SysMessage m = new SysMessage();
        m.setUserId(userId);
        m.setTitle(title);
        m.setContent(content);
        m.setBizType(bizType);
        m.setBizId(bizId);
        m.setReadFlag(0);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);
        SysUser u = userMapper.selectById(userId);
        String to = u == null ? null : (u.getEmail() == null || u.getEmail().isBlank() ? u.getUsername() + "@demo.local" : u.getEmail());
        EmailSendLog log = new EmailSendLog();
        log.setToAddr(to);
        log.setSubject(title);
        log.setContent(content);
        log.setStatus("SIMULATED_OK");
        log.setRelatedBiz(bizType);
        log.setCreatedAt(LocalDateTime.now());
        emailSendLogMapper.insert(log);
    }
}
