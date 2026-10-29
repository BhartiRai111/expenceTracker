package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;

import java.util.List;
import java.util.Map;

/**
 * All operations are scoped to a client; looking up an expense that belongs to somebody else
 * behaves as if it did not exist.
 */
public interface ExpenseService {
    Expense findExpenseById(int id, int clientId);

    void save(ExpenseDTO expenseDTO);

    void update(ExpenseDTO expenseDTO);

    List<Expense> findAllExpensesByClientId(int id);

    void deleteExpenseById(int id, int clientId);

    List<Expense> findFilterResult(int clientId, FilterDTO filter);

    Map<String, Integer> getCategorySummary(int clientId);

    Map<String, Integer> getMonthlySummary(int clientId);

    int getThisMonthExpense(int clientId);

    String getHighestCategory(int clientId);

    Map<String, Integer> getCategorySummaryByDate(int clientId, String from, String to);

    Map<String, Integer> getMonthlySummaryByDate(int clientId, String from, String to);

    List<Expense> findExpensesByDate(int clientId, String from, String to);
}
