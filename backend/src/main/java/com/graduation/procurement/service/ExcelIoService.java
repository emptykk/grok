package com.graduation.procurement.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.procurement.common.BizException;
import com.graduation.procurement.entity.Material;
import com.graduation.procurement.entity.PurchaseOrder;
import com.graduation.procurement.entity.Supplier;
import com.graduation.procurement.mapper.MaterialMapper;
import com.graduation.procurement.mapper.PurchaseOrderMapper;
import com.graduation.procurement.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelIoService {
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper orderMapper;
    private final MasterDataService masterDataService;
    private final OperLogService operLogService;

    public byte[] exportMaterials() {
        List<Material> list = materialMapper.selectList(new LambdaQueryWrapper<Material>().orderByAsc(Material::getId));
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("materials");
            Row h = sh.createRow(0);
            String[] hs = {"code","name","spec","unit","category","safeStock","remark"};
            for (int i=0;i<hs.length;i++) h.createCell(i).setCellValue(hs[i]);
            int r=1;
            for (Material m: list) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(nz(m.getCode()));
                row.createCell(1).setCellValue(nz(m.getName()));
                row.createCell(2).setCellValue(nz(m.getSpec()));
                row.createCell(3).setCellValue(nz(m.getUnit()));
                row.createCell(4).setCellValue(nz(m.getCategory()));
                row.createCell(5).setCellValue(m.getSafeStock()==null?0:m.getSafeStock().doubleValue());
                row.createCell(6).setCellValue(nz(m.getRemark()));
            }
            wb.write(out); return out.toByteArray();
        } catch (Exception e) { throw new BizException("导出失败:"+e.getMessage()); }
    }

    @Transactional
    public int importMaterials(MultipartFile file) {
        int n=0;
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sh = wb.getSheetAt(0);
            for (int i=1;i<=sh.getLastRowNum();i++) {
                Row row = sh.getRow(i); if (row==null) continue;
                String code = cell(row,0); String name = cell(row,1);
                if (code==null || code.isBlank() || name==null || name.isBlank()) continue;
                Material exist = materialMapper.selectOne(new LambdaQueryWrapper<Material>().eq(Material::getCode, code));
                Material m = exist == null ? new Material() : exist;
                m.setCode(code); m.setName(name); m.setSpec(cell(row,2)); m.setUnit(cell(row,3));
                m.setCategory(cell(row,4));
                String safe = cell(row,5); m.setSafeStock(safe==null||safe.isBlank()?BigDecimal.ZERO:new BigDecimal(safe));
                m.setRemark(cell(row,6));
                masterDataService.saveMaterial(m);
                n++;
            }
        } catch (BizException e) { throw e; }
        catch (Exception e) { throw new BizException("导入失败:"+e.getMessage()); }
        operLogService.log("物资","导入","count="+n);
        return n;
    }

    public byte[] exportSuppliers() {
        List<Supplier> list = supplierMapper.selectList(new LambdaQueryWrapper<>());
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("suppliers");
            Row h = sh.createRow(0);
            String[] hs = {"name","contact","phone","category","rating","status","admissionScore","blacklisted","remark"};
            for (int i=0;i<hs.length;i++) h.createCell(i).setCellValue(hs[i]);
            int r=1;
            for (Supplier s: list) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(nz(s.getName()));
                row.createCell(1).setCellValue(nz(s.getContact()));
                row.createCell(2).setCellValue(nz(s.getPhone()));
                row.createCell(3).setCellValue(nz(s.getCategory()));
                row.createCell(4).setCellValue(s.getRating()==null?0:s.getRating());
                row.createCell(5).setCellValue(s.getStatus()==null?0:s.getStatus());
                row.createCell(6).setCellValue(s.getAdmissionScore()==null?0:s.getAdmissionScore().doubleValue());
                row.createCell(7).setCellValue(s.getBlacklisted()==null?0:s.getBlacklisted());
                row.createCell(8).setCellValue(nz(s.getRemark()));
            }
            wb.write(out); return out.toByteArray();
        } catch (Exception e) { throw new BizException("导出失败:"+e.getMessage()); }
    }

    public byte[] exportOrders() {
        List<PurchaseOrder> list = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>().orderByDesc(PurchaseOrder::getCreatedAt));
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("orders");
            Row h = sh.createRow(0);
            String[] hs = {"orderNo","requestId","supplierId","totalAmount","status","expectDate","remark"};
            for (int i=0;i<hs.length;i++) h.createCell(i).setCellValue(hs[i]);
            int r=1;
            for (PurchaseOrder o: list) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(nz(o.getOrderNo()));
                row.createCell(1).setCellValue(o.getRequestId()==null?0:o.getRequestId());
                row.createCell(2).setCellValue(o.getSupplierId()==null?0:o.getSupplierId());
                row.createCell(3).setCellValue(o.getTotalAmount()==null?0:o.getTotalAmount().doubleValue());
                row.createCell(4).setCellValue(nz(o.getStatus()));
                row.createCell(5).setCellValue(o.getExpectDate()==null?"":o.getExpectDate().toString());
                row.createCell(6).setCellValue(nz(o.getRemark()));
            }
            wb.write(out); return out.toByteArray();
        } catch (Exception e) { throw new BizException("导出失败:"+e.getMessage()); }
    }

    private String cell(Row row, int i) {
        Cell c = row.getCell(i); if (c==null) return null;
        DataFormatter fmt = new DataFormatter();
        return fmt.formatCellValue(c);
    }
    private String nz(String s){ return s==null?"":s; }
}
