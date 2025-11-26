package com.example.trackerdetails.controller;

import com.example.trackerdetails.model.Expenditure;
import com.example.trackerdetails.service.ExpenditureService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Controller
public class ExpenditureController {
    private final ExpenditureService service;

    public ExpenditureController(ExpenditureService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) String id, Model model) {
        if (id != null) {
            service.findById(id).ifPresent(e -> model.addAttribute("exp", e));
        }
        return "form";
    }

    @GetMapping("/api/expenditures")
    @ResponseBody
    public List<Expenditure> getAll() {
        return service.findAll();
    }

    @PostMapping("/api/expenditures")
    @ResponseBody
    public Expenditure create(@RequestBody Expenditure exp) {
        return service.save(exp);
    }

    @PutMapping("/api/expenditures/{id}")
    @ResponseBody
    public ResponseEntity<Expenditure> update(@PathVariable String id, @RequestBody Expenditure exp) {
        return service.findById(id).map(existing -> {
            existing.setMonth(exp.getMonth());
            existing.setYear(exp.getYear());
            existing.setAmount(exp.getAmount());
            existing.setSavings(exp.getSavings());
            existing.setCategory(exp.getCategory());
            existing.setNotes(exp.getNotes());
            service.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/expenditures/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/excel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = "monthly_expenditures.xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        List<Expenditure> all = service.findAll();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Expenditures");
            int rowNum = 0;
            Row header = sheet.createRow(rowNum++);
            String[] cols = new String[]{"Month","Year","Amount","Savings","Category","Notes"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
            }

            for (Expenditure e : all) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(e.getMonth());
                r.createCell(1).setCellValue(e.getYear());
                r.createCell(2).setCellValue(e.getAmount());
                r.createCell(3).setCellValue(e.getSavings());
                r.createCell(4).setCellValue(e.getCategory());
                r.createCell(5).setCellValue(e.getNotes());
            }

            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
