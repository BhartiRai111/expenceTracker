package com.SpringBootMVC.ExpensesTracker.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExpenseDTO {
    private int expenseId;
    private int clientId;
    @NotBlank(message = "Category is required")
    private String category;
    @Min(value = 1, message = "Amount must be at least 1")
    private int amount;
    @NotBlank(message = "Date and time are required")
    private String dateTime;
    @Size(max = 400, message = "Description can be at most 400 characters")
    private String description;

    public ExpenseDTO() {
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ExpenseDTO{" +
                "clientId=" + clientId +
                ", category='" + category + '\'' +
                ", amount=" + amount +
                ", dateTime='" + dateTime + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
