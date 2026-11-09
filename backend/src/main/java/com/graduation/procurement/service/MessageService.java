package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.procurement.entity.EmailSendLog;
import com.graduation.procurement.entity.SysMessage;
import com.graduation.procurement.mapper.EmailSendLogMapper;
import com.graduation.procurement.mapper.SysMessageMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final SysMessageMapper messageMapper;
    private final EmailSendLogMapper emailSendLogMapper;

    public Page<SysMessage> page(int page, int size, boolean onlyUnread) {
        Long uid = SecurityUtils.currentUser().getId();
        LambdaQueryWrapper<SysMessage> q = new LambdaQueryWrapper<SysMessage>().eq(SysMessage::getUserId, uid);
        if (onlyUnread) q.eq(SysMessage::getReadFlag, 0);
        q.orderByDesc(SysMessage::getCreatedAt);
        return messageMapper.selectPage(new Page<>(page, size), q);
    }

    public Map<String, Object> unreadCount() {
        Long uid = SecurityUtils.currentUser().getId();
        long c = messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>().eq(SysMessage::getUserId, uid).eq(SysMessage::getReadFlag, 0));
        Map<String, Object> m = new HashMap<>();
        m.put("count", c);
        return m;
    }

    @Transactional
    public void read(Long id) {
        SysMessage m = messageMapper.selectById(id);
        if (m == null) return;
        if (!m.getUserId().equals(SecurityUtils.currentUser().getId()) && !SecurityUtils.hasRole("ADMIN")) return;
        m.setReadFlag(1);
        messageMapper.updateById(m);
    }

    @Transactional
    public void readAll() {
        Long uid = SecurityUtils.currentUser().getId();
        ListUpdate(uid);
    }

    private void ListUpdate(Long uid) {
        java.util.List<SysMessage> list = messageMapper.selectList(new LambdaQueryWrapper<SysMessage>().eq(SysMessage::getUserId, uid).eq(SysMessage::getReadFlag, 0));
        for (SysMessage m : list) { m.setReadFlag(1); messageMapper.updateById(m); }
    }

    public Page<EmailSendLog> emailLogs(int page, int size) {
        if (!SecurityUtils.hasRole("ADMIN")) throw new com.graduation.procurement.common.BizException("仅管理员");
        return emailSendLogMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<EmailSendLog>().orderByDesc(EmailSendLog::getCreatedAt));
    }
}
