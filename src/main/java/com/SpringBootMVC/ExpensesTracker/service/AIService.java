package com.SpringBootMVC.ExpensesTracker.service;

public interface AIService {
    /** Returns one of the known category names, or "Other" when no suggestion is possible. */
    String predictCategory(String description);

    String getChatResponse(String message);
}
