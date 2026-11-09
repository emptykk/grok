package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.MaterialTemplate;
import com.graduation.procurement.mapper.MaterialTemplateMapper;
import com.graduation.procurement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialTemplateService {
    private final MaterialTemplateMapper mapper;
    private final OperLogService operLogService;

    public List<MaterialTemplate> mine() {
        Long uid = SecurityUtils.currentUser().getId();
        return mapper.selectList(new LambdaQueryWrapper<MaterialTemplate>().eq(MaterialTemplate::getUserId, uid).orderByDesc(MaterialTemplate::getUseCount));
    }

    @Transactional
    public Long save(MaterialTemplate t) {
        t.setUserId(SecurityUtils.currentUser().getId());
        if (t.getUseCount() == null) t.setUseCount(0);
        t.setCreatedAt(LocalDateTime.now());
        mapper.insert(t);
        operLogService.log("物资模板", "保存", t.getName());
        return t.getId();
    }

    @Transactional
    public MaterialTemplate use(Long id) {
        MaterialTemplate t = mapper.selectById(id);
        if (t == null) throw new BizException("模板不存在");
        if (!t.getUserId().equals(SecurityUtils.currentUser().getId()) && !SecurityUtils.hasRole("ADMIN")) throw new BizException("无权使用他人模板");
        t.setUseCount((t.getUseCount() == null ? 0 : t.getUseCount()) + 1);
        t.setLastUsedAt(LocalDateTime.now());
        mapper.updateById(t);
        return t;
    }

    @Transactional
    public void delete(Long id) {
        MaterialTemplate t = mapper.selectById(id);
        if (t == null) return;
        if (!t.getUserId().equals(SecurityUtils.currentUser().getId()) && !SecurityUtils.hasRole("ADMIN")) throw new BizException("无权删除");
        mapper.deleteById(id);
    }

    @Transactional
    public void rememberFromItems(Long userId, List<MapLike> items) {
        if (items == null) return;
        for (MapLike it : items) {
            if (it.materialName == null || it.materialName.isBlank()) continue;
            MaterialTemplate exist = mapper.selectOne(new LambdaQueryWrapper<MaterialTemplate>()
                    .eq(MaterialTemplate::getUserId, userId).eq(MaterialTemplate::getMaterialName, it.materialName).last("LIMIT 1"));
            if (exist == null) {
                MaterialTemplate t = new MaterialTemplate();
                t.setUserId(userId); t.setName(it.materialName); t.setMaterialId(it.materialId);
                t.setMaterialName(it.materialName); t.setSpec(it.spec); t.setUnit(it.unit);
                t.setDefaultQty(it.quantity); t.setDefaultPrice(it.estimatePrice); t.setUseCount(1);
                t.setLastUsedAt(LocalDateTime.now()); t.setCreatedAt(LocalDateTime.now());
                mapper.insert(t);
            } else {
                exist.setUseCount((exist.getUseCount() == null ? 0 : exist.getUseCount()) + 1);
                exist.setLastUsedAt(LocalDateTime.now());
                if (it.estimatePrice != null) exist.setDefaultPrice(it.estimatePrice);
                mapper.updateById(exist);
            }
        }
    }

    public static class MapLike {
        public Long materialId; public String materialName; public String spec; public String unit;
        public java.math.BigDecimal quantity; public java.math.BigDecimal estimatePrice;
    }
}
