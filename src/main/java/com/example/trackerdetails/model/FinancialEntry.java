package com.example.trackerdetails.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "financial_entries")
public class FinancialEntry {
    @Id
    private String id;
    private int year;
    private int month; // 1-12
    private String type; // SPENDING, INVESTMENT, INTEREST, LOAN
    private String category;
    private double amount;

    public FinancialEntry() {}

    public FinancialEntry(int year, int month, String type, String category, double amount) {
        this.year = year;
        this.month = month;
        this.type = type;
        this.category = category;
        this.amount = amount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
