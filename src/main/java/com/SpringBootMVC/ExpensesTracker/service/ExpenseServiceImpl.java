package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.repository.ExpenseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    ExpenseRepository expenseRepository;
    ClientService clientService;
    CategoryService categoryService;
    EntityManager entityManager;
    AlertService alertService;

    @Autowired
    public ExpenseServiceImpl(ExpenseRepository expenseRepository, ClientService clientService
            , CategoryService categoryService, EntityManager entityManager, AlertService alertService) {
        this.expenseRepository = expenseRepository;
        this.clientService = clientService;
        this.categoryService = categoryService;
        this.entityManager = entityManager;
        this.alertService = alertService;
    }


    @Override
    public Expense findExpenseById(int id) {
        return expenseRepository.findById(id).orElse(null);
    }

    @Transactional
    @Override
    public void save(ExpenseDTO expenseDTO) {
        System.out.println(expenseDTO);
        Expense expense = new Expense();
        expense.setAmount(expenseDTO.getAmount());
        expense.setDateTime(expenseDTO.getDateTime());
        expense.setDescription(expenseDTO.getDescription());
        expense.setClient(clientService.findClientById(expenseDTO.getClientId()));
        Category category = categoryService.findCategoryByName(expenseDTO.getCategory());
        expense.setCategory(category);
        expenseRepository.save(expense);
        // 🔥 NEW CODE START
        // 👇 client fetch
        var client = clientService.findClientById(expenseDTO.getClientId());
        expense.setClient(client);
        int totalExpense = getThisMonthExpense(client.getId());
        if (totalExpense > client.getBudget() && !client.isAlertSent()) {
            alertService.sendAlert(client.getId(), totalExpense);
            client.setAlertSent(true);
            clientService.saveClient(client);
        }
    }

    @Override
    public void update(ExpenseDTO expenseDTO) {
        Expense existingExpense = expenseRepository.findById(expenseDTO.getExpenseId()).orElse(null);
        existingExpense.setAmount(expenseDTO.getAmount());
        existingExpense.setDateTime(expenseDTO.getDateTime());
        existingExpense.setDescription(expenseDTO.getDescription());
        Category category = categoryService.findCategoryByName(expenseDTO.getCategory());
        existingExpense.setCategory(category);
        expenseRepository.save(existingExpense);
    }

    @Override
    public List<Expense> findAllExpenses() {
        return expenseRepository.findAll();
    }

    @Override
    public List<Expense> findAllExpensesByClientId(int id) {
        return expenseRepository.findByClientId(id);
    }

    @Override
    public void deleteExpenseById(int id) {
        expenseRepository.deleteById(id);
    }

    @Override
    public List<Expense> findFilterResult(FilterDTO filter) {
        String query = "select e from Expense e where";
        if (!"all".equals(filter.getCategory())) {
            String category = filter.getCategory();
            int categoryId = categoryService.findCategoryByName(category).getId();
            query += String.format(" e.category.id = %d AND", categoryId);
        }
        int from = filter.getFrom();
        int to = filter.getTo();
        query += String.format(" e.amount between %d and %d", from, to);
        if (!"all".equals(filter.getYear())) {
            query += String.format(" AND CAST(SUBSTRING(e.dateTime, 1, 4) AS INTEGER) = %s", filter.getYear());
        }
        if (!"all".equals(filter.getMonth())) {
            query += String.format(" AND CAST(SUBSTRING(e.dateTime, 6, 2) AS INTEGER) = %s", filter.getMonth());
        }
        TypedQuery<Expense> expenseTypedQuery = entityManager.createQuery(query, Expense.class);
        List<Expense> expenseList = expenseTypedQuery.getResultList();
        return expenseList;
    }

    @Override
    public Map<String, Integer> getCategorySummary(int clientId) {

        List<Expense> list = expenseRepository.findByClientId(clientId);

        Map<String, Integer> map = new HashMap<>();

        for (Expense e : list) {
            String category = e.getCategory().getName();
            map.put(category, map.getOrDefault(category, 0) + e.getAmount());
        }

        return map;
    }

    @Override
    public Map<String, Integer> getMonthlySummary(int clientId) {

        List<Expense> list = expenseRepository.findByClientId(clientId);

        Map<String, Integer> map = new HashMap<>();

        for (Expense e : list) {

            // "2026-04-19T12:30"
            String month = e.getDateTime().substring(0, 7); // YYYY-MM

            map.put(month, map.getOrDefault(month, 0) + e.getAmount());
        }

        return map;
    }

    @Override
    public int getThisMonthExpense(int clientId) {

        Map<String, Integer> map = getMonthlySummary(clientId);

        String currentMonth = java.time.LocalDate.now().toString().substring(0, 7);

        return map.getOrDefault(currentMonth, 0);
    }

    @Override
    public String getHighestCategory(int clientId) {
        Map<String, Integer> map = getCategorySummary(clientId);
        return map.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    @Override
    public Map<String, Integer> getCategorySummaryByDate(int clientId, String from, String to) {
        List<Expense> list = expenseRepository.findByClientId(clientId);
        Map<String, Integer> map = new HashMap<>();
        for (Expense e : list) {
            String date = e.getDateTime().substring(0, 10);
            if (date.compareTo(from) >= 0 && date.compareTo(to) <= 0) {
                String category = e.getCategory().getName();
                map.put(category, map.getOrDefault(category, 0) + e.getAmount());
            }
        }
        return map;
    }

    @Override
    public Map<String, Integer> getMonthlySummaryByDate(int clientId, String from, String to) {
        List<Expense> list = expenseRepository.findByClientId(clientId);
        Map<String, Integer> map = new HashMap<>();
        for (Expense e : list) {
            String date = e.getDateTime().substring(0, 10);
            if (date.compareTo(from) >= 0 && date.compareTo(to) <= 0) {
                String month = e.getDateTime().substring(0, 7);
                map.put(month, map.getOrDefault(month, 0) + e.getAmount());
            }
        }
        return map;
    }

    @Override
    public List<Expense> findExpensesByDate(int clientId, String from, String to) {
        List<Expense> list = expenseRepository.findByClientId(clientId);
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        return list.stream().filter(e -> {
            LocalDate expenseDate = LocalDate.parse(e.getDateTime().substring(0, 10));
            return !expenseDate.isBefore(fromDate) && !expenseDate.isAfter(toDate);
        }).toList();
    }

}
