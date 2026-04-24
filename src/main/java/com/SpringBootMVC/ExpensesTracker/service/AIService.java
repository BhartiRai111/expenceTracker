package com.SpringBootMVC.ExpensesTracker.service;

public interface AIService {
    String predictCategory(String description);
    String getChatResponse(String message);
}
