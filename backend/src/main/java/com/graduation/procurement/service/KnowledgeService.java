package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.procurement.ai.DeepSeekClient;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.KnowledgeChunk;
import com.graduation.procurement.entity.KnowledgeDoc;
import com.graduation.procurement.entity.KnowledgeQa;
import com.graduation.procurement.mapper.KnowledgeChunkMapper;
import com.graduation.procurement.mapper.KnowledgeDocMapper;
import com.graduation.procurement.mapper.KnowledgeQaMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private final KnowledgeDocMapper docMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeQaMapper qaMapper;
    private final DeepSeekClient deepSeekClient;
    private final OperLogService operLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${procurement.upload-dir:./uploads}")
    private String uploadDir;

    public List<KnowledgeDoc> listDocs() {
        return docMapper.selectList(new LambdaQueryWrapper<KnowledgeDoc>().orderByDesc(KnowledgeDoc::getCreatedAt));
    }

    @Transactional
    public KnowledgeDoc upload(MultipartFile file, String title) throws Exception {
        if (!SecurityUtils.hasAnyRole("ADMIN", "PURCHASE_MANAGER")) throw new BizException("需要管理员或采购负责人权限");
        String original = file.getOriginalFilename() == null ? "doc" : file.getOriginalFilename();
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("knowledge");
        Files.createDirectories(dir);
        String saved = System.currentTimeMillis() + "_" + original.replaceAll("[\\\\/]+", "_");
        Path target = dir.resolve(saved);
        Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        String text = extractText(original, target);
        if (text == null || text.isBlank()) throw new BizException("无法从文件提取文本，请上传 PDF/DOCX/TXT");
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setTitle(title == null || title.isBlank() ? original : title);
        doc.setFileName(original);
        doc.setFilePath(target.toAbsolutePath().toString());
        doc.setStatus("READY");
        doc.setUploaderId(SecurityUtils.currentUser().getId());
        doc.setCreatedAt(LocalDateTime.now());
        docMapper.insert(doc);
        List<String> chunks = split(text, 500, 50);
        int idx = 0;
        for (String c : chunks) {
            KnowledgeChunk kc = new KnowledgeChunk();
            kc.setDocId(doc.getId()); kc.setChunkIndex(idx++); kc.setContent(c); kc.setTokenHint(c.length());
            chunkMapper.insert(kc);
        }
        doc.setChunkCount(chunks.size());
        docMapper.updateById(doc);
        operLogService.log("知识库", "上传", doc.getTitle());
        return doc;
    }

    @Transactional
    public void delete(Long id) {
        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>().eq(KnowledgeChunk::getDocId, id));
        docMapper.deleteById(id);
        operLogService.log("知识库", "删除", String.valueOf(id));
    }

    public Map<String, Object> ask(String question) {
        List<KnowledgeChunk> all = chunkMapper.selectList(new LambdaQueryWrapper<>());
        if (all.isEmpty()) throw new BizException("知识库为空，请先上传制度文档");
        List<Scored> scored = new ArrayList<>();
        Set<String> qTokens = tokens(question);
        for (KnowledgeChunk c : all) {
            double score = score(qTokens, c.getContent());
            if (score > 0) scored.add(new Scored(c, score));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<Scored> top = scored.stream().limit(4).collect(Collectors.toList());
        if (top.isEmpty()) top = all.stream().limit(3).map(c -> new Scored(c, 0)).collect(Collectors.toList());

        StringBuilder ctx = new StringBuilder();
        List<Map<String, Object>> refs = new ArrayList<>();
        List<Map<String, Object>> highlights = new ArrayList<>();
        int i = 1;
        for (Scored s : top) {
            KnowledgeDoc doc = docMapper.selectById(s.chunk.getDocId());
            ctx.append("[").append(i).append("] 《").append(doc.getTitle()).append("》片段")
                    .append(s.chunk.getChunkIndex()).append(":\n").append(s.chunk.getContent()).append("\n\n");
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("index", i);
            ref.put("docId", doc.getId());
            ref.put("docTitle", doc.getTitle());
            ref.put("chunkIndex", s.chunk.getChunkIndex());
            ref.put("content", s.chunk.getContent());
            ref.put("snippet", s.chunk.getContent().length() > 180 ? s.chunk.getContent().substring(0, 180) + "..." : s.chunk.getContent());
            ref.put("score", s.score);
            refs.add(ref);
            for (String kw : pickKeywords(question, s.chunk.getContent())) {
                Map<String, Object> h = new LinkedHashMap<>();
                h.put("refIndex", i);
                h.put("keyword", kw);
                int pos = s.chunk.getContent().indexOf(kw);
                int from = Math.max(0, pos - 20);
                int to = Math.min(s.chunk.getContent().length(), pos + kw.length() + 40);
                h.put("snippet", s.chunk.getContent().substring(from, to));
                highlights.add(h);
            }
            i++;
        }

        String answer;
        try {
            String system = "你是企业采购制度助手。仅依据给定制度片段回答，并在答案中引用片段编号如[1]。若片段不足以回答，请明确说明。不要编造制度条款。";
            String user = "制度片段:\n" + ctx + "\n问题:" + question;
            answer = deepSeekClient.chat(system, user);
        } catch (BizException e) {
            answer = "（大模型未配置）根据检索到的制度片段，相关内容如下：\n" + top.stream().map(s -> s.chunk.getContent()).collect(Collectors.joining("\n---\n"))
                    + "\n\n提示: " + e.getMessage();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer);
        result.put("references", refs);
        result.put("highlights", highlights);

        try {
            KnowledgeQa qa = new KnowledgeQa();
            qa.setUserId(SecurityUtils.currentUser().getId());
            qa.setQuestion(question);
            qa.setAnswer(answer);
            qa.setReferencesJson(objectMapper.writeValueAsString(refs));
            qa.setHighlightsJson(objectMapper.writeValueAsString(highlights));
            qa.setCreatedAt(LocalDateTime.now());
            qaMapper.insert(qa);
            result.put("qaId", qa.getId());
        } catch (Exception ignored) {}
        return result;
    }

    public Page<KnowledgeQa> qaHistory(int page, int size) {
        Long uid = SecurityUtils.currentUser().getId();
        LambdaQueryWrapper<KnowledgeQa> q = new LambdaQueryWrapper<>();
        if (!SecurityUtils.hasRole("ADMIN")) q.eq(KnowledgeQa::getUserId, uid);
        q.orderByDesc(KnowledgeQa::getCreatedAt);
        return qaMapper.selectPage(new Page<>(page, size), q);
    }

    public KnowledgeQa qaDetail(Long id) {
        KnowledgeQa qa = qaMapper.selectById(id);
        if (qa == null) throw new BizException("记录不存在");
        return qa;
    }

    private List<String> pickKeywords(String question, String content) {
        List<String> hits = new ArrayList<>();
        if (question == null || content == null) return hits;
        String q = question.replaceAll("[\\s，。？?、；;！!的了是在与及和]", "");
        for (int len = Math.min(8, q.length()); len >= 2; len--) {
            for (int i = 0; i + len <= q.length(); i++) {
                String w = q.substring(i, i + len);
                if (!content.contains(w)) continue;
                boolean covered = hits.stream().anyMatch(h -> h.contains(w));
                if (covered) continue;
                hits.removeIf(h -> w.contains(h));
                hits.add(w);
                if (hits.size() >= 4) return hits;
            }
        }
        return hits;
    }

    private String extractText(String name, Path path) throws Exception {
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(path.toFile())) { return new PDFTextStripper().getText(doc); }
        }
        if (lower.endsWith(".docx")) {
            try (InputStream in = Files.newInputStream(path); XWPFDocument doc = new XWPFDocument(in); XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                return ex.getText();
            }
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return Files.readString(path, StandardCharsets.UTF_8);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private List<String> split(String text, int size, int overlap) {
        text = text.replace("\r", "").trim();
        List<String> list = new ArrayList<>();
        if (text.length() <= size) { list.add(text); return list; }
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + size, text.length());
            list.add(text.substring(i, end));
            if (end == text.length()) break;
            i = Math.max(end - overlap, i + 1);
        }
        return list;
    }

    private Set<String> tokens(String s) {
        Set<String> set = new HashSet<>();
        if (s == null) return set;
        MatcherLike.addChineseGrams(s, set);
        for (String w : s.toLowerCase().split("[^a-zA-Z0-9\u4e00-\u9fa5]+")) {
            if (w.length() >= 2) set.add(w);
        }
        return set;
    }

    private double score(Set<String> q, String content) {
        Set<String> c = tokens(content);
        if (q.isEmpty() || c.isEmpty()) return 0;
        long inter = q.stream().filter(c::contains).count();
        return inter * 1.0 / Math.sqrt(q.size() * c.size());
    }

    private static class Scored {
        final KnowledgeChunk chunk; final double score;
        Scored(KnowledgeChunk chunk, double score) { this.chunk = chunk; this.score = score; }
    }
    private static class MatcherLike {
        static void addChineseGrams(String s, Set<String> set) {
            String cn = Pattern.compile("[^\u4e00-\u9fa5]").matcher(s).replaceAll("");
            for (int i = 0; i < cn.length(); i++) {
                set.add(String.valueOf(cn.charAt(i)));
                if (i + 1 < cn.length()) set.add(cn.substring(i, i + 2));
            }
        }
    }
}
