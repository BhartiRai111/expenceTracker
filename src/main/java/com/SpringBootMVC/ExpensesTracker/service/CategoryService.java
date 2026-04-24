package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.entity.Category;

import java.util.List;

public interface CategoryService {
    Category findCategoryByName(String name);
    Category findCategoryById(int id);
    List<Category> findAll();
}
