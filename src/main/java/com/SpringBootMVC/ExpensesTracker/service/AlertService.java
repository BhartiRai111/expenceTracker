package com.SpringBootMVC.ExpensesTracker.service;

public interface AlertService {
    void sendAlert(int clientId, int totalExpense);
}
