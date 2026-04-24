package com.SpringBootMVC.ExpensesTracker.repository;

import com.SpringBootMVC.ExpensesTracker.entity.ContactConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactConfigRepository extends JpaRepository<ContactConfig, Integer> {
    ContactConfig findByUserId(int userId);
}