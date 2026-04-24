package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.DTO.ContactInfoDTO;

public interface ContactService {
    void updateContact(String username, ContactInfoDTO dto);
}