package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    ExpenseRepository expenseRepository;
    ClientService clientService;
    CategoryService categoryService;
    AlertService alertService;

    @Autowired
    public ExpenseServiceImpl(ExpenseRepository expenseRepository, ClientService clientService
            , CategoryService categoryService, AlertService alertService) {
        this.expenseRepository = expenseRepository;
        this.clientService = clientService;
        this.categoryService = categoryService;
        this.alertService = alertService;
    }

    @Override
    public Expense findExpenseById(int id, int clientId) {
        return expenseRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    @Transactional
    @Override
    public void save(ExpenseDTO expenseDTO) {
        Client client = requireClient(expenseDTO.getClientId());
        Expense expense = new Expense();
        apply(expense, expenseDTO);
        expense.setClient(client);
        expenseRepository.save(expense);
        checkBudget(client);
    }

    @Transactional
    @Override
    public void update(ExpenseDTO expenseDTO) {
        Expense existing = findExpenseById(expenseDTO.getExpenseId(), expenseDTO.getClientId());
        apply(existing, expenseDTO);
        expenseRepository.save(existing);
        checkBudget(existing.getClient());
    }

    @Transactional
    @Override
    public void deleteExpenseById(int id, int clientId) {
        Expense existing = findExpenseById(id, clientId);
        expenseRepository.delete(existing);
        checkBudget(existing.getClient());
    }

    @Override
    public List<Expense> findAllExpensesByClientId(int id) {
        return expenseRepository.findByClientId(id);
    }

    @Override
    public List<Expense> findFilterResult(int clientId, FilterDTO filter) {
        Integer categoryId = null;
        if (!isAll(filter.getCategory())) {
            Category category = categoryService.findCategoryByName(filter.getCategory());
            if (category == null) {
                return List.of();
            }
            categoryId = category.getId();
        }
        String year = isAll(filter.getYear()) ? null : filter.getYear().trim();
        String month = null;
        if (!isAll(filter.getMonth())) {
            month = String.format("%02d", parseMonth(filter.getMonth()));
        }
        return expenseRepository.filter(clientId, categoryId, filter.getFrom(), filter.getTo(), year, month);
    }

    @Override
    public Map<String, Integer> getCategorySummary(int clientId) {
        return toMap(expenseRepository.sumByCategory(clientId));
    }

    @Override
    public Map<String, Integer> getMonthlySummary(int clientId) {
        return toMap(expenseRepository.sumByMonth(clientId));
    }

    @Override
    public int getThisMonthExpense(int clientId) {
        return (int) expenseRepository.sumForMonth(clientId, LocalDate.now().toString().substring(0, 7));
    }

    @Override
    public String getHighestCategory(int clientId) {
        return getCategorySummary(clientId).entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    @Override
    public Map<String, Integer> getCategorySummaryByDate(int clientId, String from, String to) {
        validateRange(from, to);
        return toMap(expenseRepository.sumByCategoryBetween(clientId, from, to));
    }

    @Override
    public Map<String, Integer> getMonthlySummaryByDate(int clientId, String from, String to) {
        validateRange(from, to);
        return toMap(expenseRepository.sumByMonthBetween(clientId, from, to));
    }

    @Override
    public List<Expense> findExpensesByDate(int clientId, String from, String to) {
        validateRange(from, to);
        return expenseRepository.findBetween(clientId, from, to);
    }

    /** Sends the budget alert once when the limit is crossed and re-arms it when spend drops back. */
    private void checkBudget(Client client) {
        int total = getThisMonthExpense(client.getId());
        if (total > client.getBudget() && !client.isAlertSent()) {
            client.setAlertSent(true);
            clientService.saveClient(client);
            alertService.sendAlert(client, total);
        } else if (total <= client.getBudget() && client.isAlertSent()) {
            client.setAlertSent(false);
            clientService.saveClient(client);
        }
    }

    private void apply(Expense expense, ExpenseDTO dto) {
        Category category = categoryService.findCategoryByName(dto.getCategory());
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category: " + dto.getCategory());
        }
        try {
            LocalDateTime.parse(dto.getDateTime());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date/time");
        }
        expense.setAmount(dto.getAmount());
        expense.setDateTime(dto.getDateTime());
        expense.setDescription(dto.getDescription());
        expense.setCategory(category);
    }

    private Client requireClient(int clientId) {
        Client client = clientService.findClientById(clientId);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        return client;
    }

    private static boolean isAll(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value);
    }

    private static int parseMonth(String value) {
        try {
            int month = Integer.parseInt(value.trim());
            if (month < 1 || month > 12) throw new NumberFormatException();
            return month;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month");
        }
    }

    private static void validateRange(String from, String to) {
        try {
            LocalDate.parse(from);
            LocalDate.parse(to);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates must be yyyy-MM-dd");
        }
    }

    private static Map<String, Integer> toMap(List<Object[]> rows) {
        Map<String, Integer> map = new TreeMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }
}
