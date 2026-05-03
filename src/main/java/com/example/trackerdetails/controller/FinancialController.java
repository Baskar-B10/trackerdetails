package com.example.trackerdetails.controller;

import com.example.trackerdetails.model.CustomCategory;
import com.example.trackerdetails.model.FinancialEntry;
import com.example.trackerdetails.repository.CustomCategoryRepository;
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
    private final CustomCategoryRepository customCategoryRepository;

    public FinancialController(FinancialService service, CustomCategoryRepository customCategoryRepository) {
        this.service = service;
        this.customCategoryRepository = customCategoryRepository;
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
        // Always include last 3 years and next year
        for (int y = currentYear - 2; y <= currentYear + 1; y++) {
            if (!years.contains(y)) years.add(y);
        }
        years.sort(Comparator.reverseOrder());
        return years;
    }

    // ── Custom Categories ────────────────────────────────────────────────────

    @GetMapping("/api/categories")
    @ResponseBody
    public List<CustomCategory> getCustomCategories(@RequestParam String type) {
        return customCategoryRepository.findByTypeOrderBySortOrderAsc(type);
    }

    @PostMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<CustomCategory> addCustomCategory(@RequestBody Map<String, String> body) {
        String type = body.get("type");
        String name = body.get("name").trim();
        if (name.isEmpty()) return ResponseEntity.badRequest().build();
        if (customCategoryRepository.existsByTypeAndName(type, name)) {
            return ResponseEntity.status(409).build();
        }
        long count = customCategoryRepository.findByTypeOrderBySortOrderAsc(type).size();
        CustomCategory cat = new CustomCategory(type, name, (int) count);
        return ResponseEntity.ok(customCategoryRepository.save(cat));
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCustomCategory(@PathVariable String id) {
        customCategoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/excel")
    public void exportExcel(@RequestParam int year, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=financial_" + year + ".xlsx");
        // Collect all custom category names per type
        Map<String, List<String>> customCats = new LinkedHashMap<>();
        for (String t : List.of("SPENDING","INVESTMENT","INTEREST","LOAN")) {
            List<String> names = customCategoryRepository.findByTypeOrderBySortOrderAsc(t)
                    .stream().map(CustomCategory::getName).toList();
            customCats.put(t, names);
        }
        try (XSSFWorkbook wb = service.exportToExcel(year, customCats)) {
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
