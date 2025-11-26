package com.example.trackerdetails.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "expenditures")
public class Expenditure {
    @Id
    private String id;
    private String month; // e.g., "January"
    private int year;
    private double amount;
    private double savings;
    private String category;
    private String notes;

    public Expenditure() {}

    public Expenditure(String month, int year, double amount, double savings, String category, String notes) {
        this.month = month;
        this.year = year;
        this.amount = amount;
        this.savings = savings;
        this.category = category;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getSavings() {
        return savings;
    }

    public void setSavings(double savings) {
        this.savings = savings;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
