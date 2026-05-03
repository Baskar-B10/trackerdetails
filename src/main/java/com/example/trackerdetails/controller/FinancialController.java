package com.example.trackerdetails.controller;

import com.example.trackerdetails.model.FinancialEntry;
import com.example.trackerdetails.service.FinancialService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Controller
public class FinancialController {

    private final FinancialService service;

    public FinancialController(FinancialService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/api/financial/matrix")
    @ResponseBody
    public Map<String, Object> getMatrix(@RequestParam int year, @RequestParam String type) {
        Map<String, double[]> matrix = service.getMatrix(year, type);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("year", year);
        response.put("type", type);
        response.put("data", matrix);
        return response;
    }

    @PostMapping("/api/financial/entry")
    @ResponseBody
    public ResponseEntity<FinancialEntry> upsertEntry(@RequestBody Map<String, Object> body) {
        int year = (int) body.get("year");
        int month = (int) body.get("month");
        String type = (String) body.get("type");
        String category = (String) body.get("category");
        double amount = ((Number) body.get("amount")).doubleValue();
        FinancialEntry entry = service.upsertEntry(year, month, type, category, amount);
        return ResponseEntity.ok(entry);
    }

    @DeleteMapping("/api/financial/entry/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteEntry(@PathVariable String id) {
        service.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/financial/summary")
    @ResponseBody
    public Map<String, Object> getSummary(@RequestParam int year) {
        return service.getSummary(year);
    }

    @GetMapping("/api/financial/years")
    @ResponseBody
    public List<Integer> getYears() {
        List<Integer> years = service.getDistinctYears();
        int currentYear = LocalDate.now().getYear();
        if (!years.contains(currentYear)) {
            years.add(0, currentYear);
        }
        years.sort(Comparator.reverseOrder());
        return years;
    }

    @GetMapping("/api/financial/categories")
    @ResponseBody
    public Map<String, List<String>> getCategories() {
        Map<String, List<String>> cats = new LinkedHashMap<>();
        cats.put("SPENDING", FinancialService.SPENDING_CATEGORIES);
        cats.put("INVESTMENT", FinancialService.INVESTMENT_CATEGORIES);
        cats.put("INTEREST", FinancialService.INTEREST_CATEGORIES);
        cats.put("LOAN", FinancialService.LOAN_CATEGORIES);
        return cats;
    }

    @GetMapping("/export/excel")
    public void exportExcel(@RequestParam int year, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=financial_" + year + ".xlsx");
        try (XSSFWorkbook wb = service.exportToExcel(year)) {
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
