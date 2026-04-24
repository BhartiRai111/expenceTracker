package com.SpringBootMVC.ExpensesTracker.service;

public interface SmsService {
    void sendSms(String to, String message);
}
