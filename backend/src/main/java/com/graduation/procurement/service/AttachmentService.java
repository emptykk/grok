package com.graduation.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.entity.Attachment;
import com.graduation.procurement.mapper.AttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentMapper attachmentMapper;

    @Value("${procurement.upload-dir:./uploads}")
    private String uploadDir;

    public Attachment upload(String bizType, Long bizId, MultipartFile file) throws Exception {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("attachments");
        Files.createDirectories(dir);
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String saved = System.currentTimeMillis() + "_" + original.replaceAll("[\\\\/]+", "_");
        Path target = dir.resolve(saved);
        Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Attachment a = new Attachment();
        a.setBizType(bizType);
        a.setBizId(bizId);
        a.setFileName(original);
        a.setFilePath(target.toAbsolutePath().toString());
        a.setSize(file.getSize());
        a.setCreatedAt(LocalDateTime.now());
        attachmentMapper.insert(a);
        return a;
    }

    public List<Attachment> list(String bizType, Long bizId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getBizType, bizType).eq(Attachment::getBizId, bizId));
    }
}
