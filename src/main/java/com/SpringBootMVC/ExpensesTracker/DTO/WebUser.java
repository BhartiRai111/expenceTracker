package com.SpringBootMVC.ExpensesTracker.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class WebUser {
    @NotNull(message = "this field is required!")
    @Size(min = 1, message = "this field is required")
    private String username;

    @NotNull(message = "this field is required!")
    @Size(min = 1, message = "this field is required")
    private String password;

    @NotNull(message = "this field is required!")
    @Size(min = 1, message = "this field is required")
    private String firstName;

    @NotNull(message = "this field is required!")
    @Size(min = 1, message = "this field is required")
    private String lastName;

    @NotNull(message = "this field is required!")
    @Size(min = 1, message = "this field is required")
    private String email;

    @NotNull(message = "income required")
    @Min(value = 1, message = "income must be > 0")
    private Integer income;

    @NotNull(message = "budget required")
    @Min(value = 1, message = "budget must be > 0")
    private Integer budget;

    private String phone;
    private String whatsappNumber;

    public WebUser() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getIncome() {
        return income;
    }

    public void setIncome(Integer income) {
        this.income = income;
    }

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }
}
