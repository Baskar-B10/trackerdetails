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

        double totalSpending = Arrays.stream(monthlySpending).sum();
        double totalInvestment = Arrays.stream(monthlyInvestment).sum();
        double totalIncome = Arrays.stream(monthlyIncome).sum();
        double totalLoan = Arrays.stream(monthlyLoan).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSpending", totalSpending);
        result.put("totalInvestment", totalInvestment);
        result.put("totalIncome", totalIncome);
        result.put("totalLoan", totalLoan);
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

    public XSSFWorkbook exportToExcel(int year) {
        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFCellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle groupStyle = createGroupStyle(wb);
        XSSFCellStyle totalStyle = createTotalStyle(wb);
        XSSFCellStyle numberStyle = createNumberStyle(wb);
        XSSFCellStyle categoryStyle = createCategoryStyle(wb);

        Map<String, double[]> spendingMatrix = getMatrix(year, "SPENDING");
        Map<String, double[]> investmentMatrix = getMatrix(year, "INVESTMENT");
        Map<String, double[]> interestMatrix = getMatrix(year, "INTEREST");
        Map<String, double[]> loanMatrix = getMatrix(year, "LOAN");

        createStandardSheet(wb, "Spending", SPENDING_CATEGORIES, spendingMatrix,
            headerStyle, totalStyle, numberStyle, categoryStyle, null, null);

        List<String> investmentMain = INVESTMENT_CATEGORIES.subList(0, 10);
        List<String> investmentSharing = INVESTMENT_CATEGORIES.subList(10, INVESTMENT_CATEGORIES.size());
        createStandardSheet(wb, "Investment & Sharing", investmentMain, investmentMatrix,
            headerStyle, totalStyle, numberStyle, categoryStyle, investmentSharing, groupStyle);

        createStandardSheet(wb, "Interest & Loan", INTEREST_CATEGORIES, interestMatrix,
            headerStyle, totalStyle, numberStyle, categoryStyle, LOAN_CATEGORIES, groupStyle);

        return wb;
    }

    private void createStandardSheet(XSSFWorkbook wb, String sheetName,
                                     List<String> mainCats, Map<String, double[]> mainMatrix,
                                     XSSFCellStyle headerStyle, XSSFCellStyle totalStyle,
                                     XSSFCellStyle numberStyle, XSSFCellStyle categoryStyle,
                                     List<String> secondaryCats, XSSFCellStyle groupStyle) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        int rowNum = 0;

        // Header row
        Row header = sheet.createRow(rowNum++);
        header.createCell(0).setCellValue("Details");
        header.getCell(0).setCellStyle(headerStyle);
        for (int m = 0; m < 12; m++) {
            Cell c = header.createCell(m + 1);
            c.setCellValue(MONTHS[m]);
            c.setCellStyle(headerStyle);
        }
        Cell totalCell = header.createCell(13);
        totalCell.setCellValue("Overall Total");
        totalCell.setCellStyle(headerStyle);
        Cell avgCell = header.createCell(14);
        avgCell.setCellValue("Monthly Avg");
        avgCell.setCellStyle(headerStyle);

        // Main categories
        double[] columnTotals = new double[12];
        for (String cat : mainCats) {
            Row row = sheet.createRow(rowNum++);
            Cell catCell = row.createCell(0);
            catCell.setCellValue(cat);
            catCell.setCellStyle(categoryStyle);

            double[] amounts = mainMatrix.getOrDefault(cat, new double[12]);
            double rowTotal = 0;
            for (int m = 0; m < 12; m++) {
                Cell c = row.createCell(m + 1);
                c.setCellValue(amounts[m]);
                c.setCellStyle(numberStyle);
                rowTotal += amounts[m];
                columnTotals[m] += amounts[m];
            }
            row.createCell(13).setCellValue(rowTotal);
            row.getCell(13).setCellStyle(totalStyle);
            row.createCell(14).setCellValue(rowTotal / 12.0);
            row.getCell(14).setCellStyle(numberStyle);
        }

        // Total row for main
        Row totalRow = sheet.createRow(rowNum++);
        Cell totLabel = totalRow.createCell(0);
        totLabel.setCellValue("Total");
        totLabel.setCellStyle(totalStyle);
        double grandTotal = 0;
        for (int m = 0; m < 12; m++) {
            Cell c = totalRow.createCell(m + 1);
            c.setCellValue(columnTotals[m]);
            c.setCellStyle(totalStyle);
            grandTotal += columnTotals[m];
        }
        totalRow.createCell(13).setCellValue(grandTotal);
        totalRow.getCell(13).setCellStyle(totalStyle);
        totalRow.createCell(14).setCellValue(grandTotal / 12.0);
        totalRow.getCell(14).setCellStyle(totalStyle);

        // Secondary section
        if (secondaryCats != null && !secondaryCats.isEmpty()) {
            rowNum++; // blank row
            Row secHeader = sheet.createRow(rowNum++);
            if (groupStyle != null) {
                secHeader.createCell(0).setCellValue(secondaryCats.equals(LOAN_CATEGORIES) ? "Loan Details" : "Sharing / Transfers");
                secHeader.getCell(0).setCellStyle(groupStyle);
            }

            double[] secColumnTotals = new double[12];
            Map<String, double[]> secMatrix = secondaryCats.equals(LOAN_CATEGORIES) ?
                getMatrix(0, "LOAN") : mainMatrix;
            // For secondary, use the right matrix
            for (String cat : secondaryCats) {
                Row row = sheet.createRow(rowNum++);
                Cell catCell = row.createCell(0);
                catCell.setCellValue(cat);
                catCell.setCellStyle(categoryStyle);

                double[] amounts = secMatrix.getOrDefault(cat, new double[12]);
                double rowTotal = 0;
                for (int m = 0; m < 12; m++) {
                    Cell c = row.createCell(m + 1);
                    c.setCellValue(amounts[m]);
                    c.setCellStyle(numberStyle);
                    rowTotal += amounts[m];
                    secColumnTotals[m] += amounts[m];
                }
                row.createCell(13).setCellValue(rowTotal);
                row.getCell(13).setCellStyle(totalStyle);
            }
        }

        // Auto-size columns
        sheet.setColumnWidth(0, 10000);
        for (int i = 1; i <= 14; i++) {
            sheet.setColumnWidth(i, 3500);
        }
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x16, (byte)0x32, (byte)0x4F}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createGroupStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x21, (byte)0x39, (byte)0x55}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createTotalStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xE8, (byte)0xF5, (byte)0xE9}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFDataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("#,##0.00"));
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createNumberStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFDataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("#,##0.00"));
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createCategoryStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(false);
        style.setFont(font);
        style.setWrapText(false);
        setBorder(style);
        return style;
    }

    private void setBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
