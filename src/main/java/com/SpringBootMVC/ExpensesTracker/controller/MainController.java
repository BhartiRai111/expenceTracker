package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.DTO.ContactInfoDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.CustomUserDetails;
import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {
    ExpenseService expenseService;
    CategoryService categoryService;
    ClientService clientService;
    ContactService contactService;

    @Autowired
    public MainController(ExpenseService expenseService, CategoryService categoryService
            , ClientService clientService, ContactService contactService) {
        this.expenseService = expenseService;
        this.categoryService = categoryService;
        this.clientService = clientService;
        this.contactService = contactService;
    }

    /** The client of the logged-in user, read fresh from the database on every request. */
    private Client currentClient(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Client client = clientService.findClientById(details.getClientId());
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        return client;
    }

    private void fillDisplayFields(List<Expense> expenses) {
        for (Expense expense : expenses) {
            expense.setCategoryName(expense.getCategory().getName());
            LocalDateTime dateTime = LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            expense.setDate(dateTime.toLocalDate().toString());
            expense.setTime(dateTime.toLocalTime().toString());
        }
    }

    @GetMapping("/")
    public String landingPage() {
        return "landing-page";
    }

    @GetMapping("/showAdd")
    public String addExpense(Model model) {
        model.addAttribute("expense", new ExpenseDTO());
        model.addAttribute("categories", categoryService.findAll());
        return "add-expense";
    }

    @PostMapping("/submitAdd")
    public String submitAdd(@Valid @ModelAttribute("expense") ExpenseDTO expenseDTO, BindingResult result,
                            Model model, Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "add-expense";
        }
        expenseDTO.setClientId(currentClient(authentication).getId());
        expenseService.save(expenseDTO);
        return "redirect:/list";
    }

    @GetMapping("/list")
    public String list(Model model, Authentication authentication) {
        List<Expense> expenseList = expenseService.findAllExpensesByClientId(currentClient(authentication).getId());
        fillDisplayFields(expenseList);
        model.addAttribute("expenseList", expenseList);
        model.addAttribute("filter", new FilterDTO());
        return "list-page";
    }

    @GetMapping("/showUpdate")
    public String showUpdate(@RequestParam("expId") int id, Model model, Authentication authentication) {
        Expense expense = expenseService.findExpenseById(id, currentClient(authentication).getId());
        ExpenseDTO expenseDTO = new ExpenseDTO();
        expenseDTO.setAmount(expense.getAmount());
        expenseDTO.setCategory(expense.getCategory().getName());
        expenseDTO.setDescription(expense.getDescription());
        expenseDTO.setDateTime(expense.getDateTime());

        model.addAttribute("expense", expenseDTO);
        model.addAttribute("expenseId", id);
        model.addAttribute("categories", categoryService.findAll());
        return "update-page";
    }

    @PostMapping("/submitUpdate")
    public String update(@RequestParam("expId") int id, @Valid @ModelAttribute("expense") ExpenseDTO expenseDTO,
                         BindingResult result, Model model, Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("expenseId", id);
            model.addAttribute("categories", categoryService.findAll());
            return "update-page";
        }
        expenseDTO.setExpenseId(id);
        expenseDTO.setClientId(currentClient(authentication).getId());
        expenseService.update(expenseDTO);
        return "redirect:/list";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("expId") int id, Authentication authentication) {
        expenseService.deleteExpenseById(id, currentClient(authentication).getId());
        return "redirect:/list";
    }

    @PostMapping("/processFilter")
    public String processFilter(@ModelAttribute("filter") FilterDTO filter, Model model,
                                Authentication authentication) {
        List<Expense> expenseList = expenseService.findFilterResult(currentClient(authentication).getId(), filter);
        fillDisplayFields(expenseList);
        model.addAttribute("expenseList", expenseList);
        return "filter-result";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        Client client = currentClient(authentication);
        int clientId = client.getId();
        Map<String, Integer> monthlyData = expenseService.getMonthlySummary(clientId);
        int thisMonth = expenseService.getThisMonthExpense(clientId);
        int budget = client.getBudget();
        int income = client.getIncome();

        model.addAttribute("categoryData", expenseService.getCategorySummary(clientId));
        model.addAttribute("monthlyData", monthlyData);
        model.addAttribute("highestCategory", expenseService.getHighestCategory(clientId));
        model.addAttribute("thisMonthExpense", thisMonth);
        model.addAttribute("budget", budget);
        model.addAttribute("isOverBudget", thisMonth > budget);
        model.addAttribute("income", income);
        model.addAttribute("savings", income - thisMonth);

        Map<String, Integer> incomeData = new HashMap<>();
        for (String month : monthlyData.keySet()) {
            incomeData.put(month, income);
        }
        model.addAttribute("incomeData", incomeData);
        model.addAttribute("percent", budget > 0 ? (thisMonth * 100.0) / budget : 0);
        return "dashboard";
    }

    @PostMapping("/setBudget")
    public String setBudget(@RequestParam("budget") int budget, Authentication authentication) {
        if (budget < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget must be greater than 0");
        }
        Client client = currentClient(authentication);
        client.setBudget(budget);
        // a new limit re-arms the alert; it will fire again on the next expense if still exceeded
        client.setAlertSent(false);
        clientService.saveClient(client);
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard/filter")
    @ResponseBody
    public Map<String, Object> filterDashboard(@RequestParam String from, @RequestParam String to,
                                               Authentication authentication) {
        int clientId = currentClient(authentication).getId();
        Map<String, Object> result = new HashMap<>();
        result.put("categoryData", expenseService.getCategorySummaryByDate(clientId, from, to));
        result.put("monthlyData", expenseService.getMonthlySummaryByDate(clientId, from, to));
        result.put("thisMonthExpense", expenseService.getThisMonthExpense(clientId));
        result.put("highestCategory", expenseService.getHighestCategory(clientId));
        return result;
    }

    @GetMapping("/dashboard/export")
    public void exportExcel(@RequestParam String from, @RequestParam(required = false) String to,
                            Authentication authentication, HttpServletResponse response) throws IOException {
        if (to == null || to.isEmpty()) {
            to = LocalDate.now().toString();
        }
        List<Expense> list = expenseService.findExpensesByDate(currentClient(authentication).getId(), from, to);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Expenses");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Amount");
            header.createCell(2).setCellValue("Date");
            header.createCell(3).setCellValue("Description");

            int rowNum = 1;
            for (Expense e : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getCategory().getName());
                row.createCell(1).setCellValue(e.getAmount());
                row.createCell(2).setCellValue(e.getDateTime());
                row.createCell(3).setCellValue(e.getDescription() == null ? "" : e.getDescription());
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=expenses.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/showContactPage")
    public String showContactPage(Model model) {
        model.addAttribute("contact", new ContactInfoDTO());
        return "contact-page";
    }

    @PostMapping("/saveContact")
    public String saveContact(@ModelAttribute("contact") ContactInfoDTO dto,
                              Authentication authentication) {
        contactService.updateContact(authentication.getName(), dto);
        return "contact-page";
    }

}
