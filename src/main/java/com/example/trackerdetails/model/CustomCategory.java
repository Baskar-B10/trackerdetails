package com.example.trackerdetails.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "custom_categories")
public class CustomCategory {
    @Id
    private String id;
    private String type;  // SPENDING, INVESTMENT, INTEREST, LOAN
    private String name;
    private int sortOrder;

    public CustomCategory() {}

    public CustomCategory(String type, String name, int sortOrder) {
        this.type = type;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
