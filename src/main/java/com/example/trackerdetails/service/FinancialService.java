package com.example.trackerdetails.service;

import com.example.trackerdetails.model.FinancialEntry;
import com.example.trackerdetails.repository.FinancialRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialService {

    private final FinancialRepository repository;

    public static final List<String> SPENDING_CATEGORIES = Arrays.asList(
        "Rent", "Electricity Bill", "Gas", "Maid", "Water Can", "Repair Works/Installation",
        "Milk", "Grocery", "Grocery (Detergents/Hair Products)", "Vegetables", "Flowers",
        "Healthy Snacks", "Hotel", "Snacks",
        "Petrol", "Travel",
        "Medical", "Grooming",
        "Recharge (Phone & Wifi)", "Other Bills (Insurance/DTH)",
        "Movie/Entertainment", "Outing/Travel for Experience",
        "Shopping (Dress/Gifts)", "Shopping for Home", "Shopping for Family",
        "Extras"
    );

    public static final List<String> INVESTMENT_CATEGORIES = Arrays.asList(
        "Gold and Silver", "SBI Life Insurance", "Recurring Deposit (RD)", "PPF",
        "Stocks", "SBI Jai Nivesh (SIP)", "Parag Flexi Cap SIP", "Large Cap Fund SIP",
        "Bond Investment (RBI + Corporate)", "NPS",
        "Sharing - To Family Member", "Sharing - To Family", "Sharing - To Relatives"
    );

    public static final List<String> INTEREST_CATEGORIES = Arrays.asList(
        "From Parents/Relatives", "Waste Plastic", "Sell Product/Coupons",
        "Gas Subsidy", "Indian Bank Interest", "SBI Interest", "ICICI Interest",
        "Post Office Interest", "Dividend", "Bond Interest", "FD/RD Interest", "Cashback"
    );

    public static final List<String> LOAN_CATEGORIES = Arrays.asList("Gold Loan", "Personal Loan", "Other Loan");

    private static final String[] MONTHS = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

    public FinancialService(FinancialRepository repository) {
        this.repository = repository;
    }

    public FinancialEntry upsertEntry(int year, int month, String type, String category, double amount) {
        Optional<FinancialEntry> existing = repository.findByYearAndMonthAndTypeAndCategory(year, month, type, category);
        FinancialEntry entry = existing.orElse(new FinancialEntry(year, month, type, category, 0));
        entry.setAmount(amount);
        return repository.save(entry);
    }

    public boolean deleteEntry(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    public Map<String, double[]> getMatrix(int year, String type) {
        List<FinancialEntry> entries = repository.findByYearAndType(year, type);
        Map<String, double[]> matrix = new LinkedHashMap<>();
        for (FinancialEntry e : entries) {
            matrix.computeIfAbsent(e.getCategory(), k -> new double[12])[e.getMonth() - 1] = e.getAmount();
        }
        return matrix;
    }

    public Map<String, Object> getSummary(int year) {
        List<FinancialEntry> all = repository.findByYear(year);

        double[] monthlySpending = new double[12];
        double[] monthlyInvestment = new double[12];
        double[] monthlyIncome = new double[12];
        double[] monthlyLoan = new double[12];
        Map<String, Double> spendingByCategory = new LinkedHashMap<>();
        Map<String, Double> investmentByCategory = new LinkedHashMap<>();

        for (FinancialEntry e : all) {
            int mi = e.getMonth() - 1;
            switch (e.getType()) {
                case "SPENDING" -> {
                    monthlySpending[mi] += e.getAmount();
                    spendingByCategory.merge(e.getCategory(), e.getAmount(), Double::sum);
                }
                case "INVESTMENT" -> {
                    monthlyInvestment[mi] += e.getAmount();
                    investmentByCategory.merge(e.getCategory(), e.getAmount(), Double::sum);
                }
                case "INTEREST" -> monthlyIncome[mi] += e.getAmount();
                case "LOAN" -> monthlyLoan[mi] += e.getAmount();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSpending", Arrays.stream(monthlySpending).sum());
        result.put("totalInvestment", Arrays.stream(monthlyInvestment).sum());
        result.put("totalIncome", Arrays.stream(monthlyIncome).sum());
        result.put("totalLoan", Arrays.stream(monthlyLoan).sum());
        result.put("monthlySpending", monthlySpending);
        result.put("monthlyInvestment", monthlyInvestment);
        result.put("monthlyIncome", monthlyIncome);
        result.put("monthlyLoan", monthlyLoan);
        result.put("spendingByCategory", spendingByCategory);
        result.put("investmentByCategory", investmentByCategory);
        return result;
    }

    public List<Integer> getDistinctYears() {
        return repository.findAll().stream()
            .map(FinancialEntry::getYear)
            .distinct()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
    }

    // ── Excel Export ─────────────────────────────────────────────────────────

    public XSSFWorkbook exportToExcel(int year, Map<String, List<String>> customCats) {
        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFCellStyle headerStyle  = createHeaderStyle(wb);
        XSSFCellStyle groupStyle   = createGroupStyle(wb);
        XSSFCellStyle totalStyle   = createTotalStyle(wb);
        XSSFCellStyle numberStyle  = createNumberStyle(wb);
        XSSFCellStyle categoryStyle = createCategoryStyle(wb);
        XSSFCellStyle customStyle  = createCustomStyle(wb);

        Map<String, double[]> spendingMatrix   = getMatrix(year, "SPENDING");
        Map<String, double[]> investmentMatrix = getMatrix(year, "INVESTMENT");
        Map<String, double[]> interestMatrix   = getMatrix(year, "INTEREST");
        Map<String, double[]> loanMatrix       = getMatrix(year, "LOAN");

        List<String> spendExtra = customCats.getOrDefault("SPENDING", List.of());
        List<String> invExtra   = customCats.getOrDefault("INVESTMENT", List.of());
        List<String> intExtra   = customCats.getOrDefault("INTEREST", List.of());
        List<String> loanExtra  = customCats.getOrDefault("LOAN", List.of());

        // Sheet 1: Spending
        createSheet(wb, "Spending", year,
            List.of(new Section("Spending", SPENDING_CATEGORIES, spendingMatrix),
                    new Section("Custom", spendExtra, spendingMatrix)),
            headerStyle, totalStyle, numberStyle, categoryStyle, customStyle, groupStyle);

        // Sheet 2: Investment & Sharing
        createSheet(wb, "Investment & Sharing", year,
            List.of(new Section("Investments", INVESTMENT_CATEGORIES.subList(0,10), investmentMatrix),
                    new Section("Sharing", INVESTMENT_CATEGORIES.subList(10, INVESTMENT_CATEGORIES.size()), investmentMatrix),
                    new Section("Custom", invExtra, investmentMatrix)),
            headerStyle, totalStyle, numberStyle, categoryStyle, customStyle, groupStyle);

        // Sheet 3: Interest & Loan
        createSheet(wb, "Interest & Loan", year,
            List.of(new Section("Interest & Income", INTEREST_CATEGORIES, interestMatrix),
                    new Section("Custom", intExtra, interestMatrix),
                    new Section("Loan Repayments", concat(LOAN_CATEGORIES, loanExtra), loanMatrix)),
            headerStyle, totalStyle, numberStyle, categoryStyle, customStyle, groupStyle);

        return wb;
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> r = new ArrayList<>(a); r.addAll(b); return r;
    }

    record Section(String label, List<String> cats, Map<String, double[]> matrix) {}

    private void createSheet(XSSFWorkbook wb, String name, int year,
                             List<Section> sections,
                             XSSFCellStyle hdr, XSSFCellStyle total,
                             XSSFCellStyle num, XSSFCellStyle cat,
                             XSSFCellStyle custom, XSSFCellStyle grp) {
        XSSFSheet sheet = wb.createSheet(name);
        int rowNum = 0;

        // Header
        Row header = sheet.createRow(rowNum++);
        cell(header, 0, "Details", hdr);
        for (int m = 0; m < 12; m++) cell(header, m+1, MONTHS[m], hdr);
        cell(header, 13, "Total " + year, hdr);
        cell(header, 14, "Monthly Avg", hdr);

        double[] grandTotals = new double[12];

        for (Section sec : sections) {
            if (sec.cats().isEmpty()) continue;

            // Section group header
            Row gh = sheet.createRow(rowNum++);
            cell(gh, 0, sec.label(), grp);

            double[] secTotals = new double[12];
            for (String catName : sec.cats()) {
                double[] amounts = sec.matrix().getOrDefault(catName, new double[12]);
                double rowTotal = 0;
                Row row = sheet.createRow(rowNum++);
                XSSFCellStyle cs = sec.label().equals("Custom") ? custom : cat;
                cell(row, 0, catName, cs);
                for (int m = 0; m < 12; m++) {
                    numCell(row, m+1, amounts[m], num);
                    rowTotal += amounts[m];
                    secTotals[m] += amounts[m];
                    grandTotals[m] += amounts[m];
                }
                numCell(row, 13, rowTotal, total);
                numCell(row, 14, rowTotal/12.0, num);
            }

            // Section subtotal
            Row subRow = sheet.createRow(rowNum++);
            double secTotal = Arrays.stream(secTotals).sum();
            cell(subRow, 0, "Subtotal - " + sec.label(), total);
            for (int m = 0; m < 12; m++) numCell(subRow, m+1, secTotals[m], total);
            numCell(subRow, 13, secTotal, total);
            numCell(subRow, 14, secTotal/12.0, total);
            rowNum++; // blank row
        }

        // Grand total
        Row gt = sheet.createRow(rowNum);
        double grandTotal = Arrays.stream(grandTotals).sum();
        cell(gt, 0, "GRAND TOTAL", total);
        for (int m = 0; m < 12; m++) numCell(gt, m+1, grandTotals[m], total);
        numCell(gt, 13, grandTotal, total);
        numCell(gt, 14, grandTotal/12.0, total);

        sheet.setColumnWidth(0, 11000);
        for (int i = 1; i <= 14; i++) sheet.setColumnWidth(i, 3600);
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void numCell(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    // ── Styles ───────────────────────────────────────────────────────────────

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x16,(byte)0x32,(byte)0x4F}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        border(s); return s;
    }

    private XSSFCellStyle createGroupStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x21,(byte)0x39,(byte)0x55}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        border(s); return s;
    }

    private XSSFCellStyle createTotalStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xE8,(byte)0xF5,(byte)0xE9}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        border(s); return s;
    }

    private XSSFCellStyle createNumberStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        border(s); return s;
    }

    private XSSFCellStyle createCategoryStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle(); border(s); return s;
    }

    private XSSFCellStyle createCustomStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setItalic(true); s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xF0,(byte)0xF8,(byte)0xFF}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        border(s); return s;
    }

    private void border(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);  s.setBorderRight(BorderStyle.THIN);
    }
}
