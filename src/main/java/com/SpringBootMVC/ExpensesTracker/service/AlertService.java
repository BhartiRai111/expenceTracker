package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.entity.Client;

public interface AlertService {
    void sendAlert(Client client, int totalExpense);
}
