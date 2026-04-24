package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.DTO.ContactInfoDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.ContactConfig;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {
    ExpenseService expenseService;
    CategoryService categoryService;
    EmailService emailService;
    ClientService clientService;
    ContactService contactService;
    UserService userService;
    SmsService smsService;

    @Autowired
    public MainController(ExpenseService expenseService, CategoryService categoryService
            , ClientService clientService, EmailService emailService,
                          ContactService contactService, UserService userService, SmsService smsService) {
        this.expenseService = expenseService;
        this.categoryService = categoryService;
        this.clientService = clientService;
        this.emailService = emailService;
        this.contactService = contactService;
        this.userService = userService;
        this.smsService = smsService;
    }

    @GetMapping("/")
    public String landingPage() {
        return "landing-page";
    }

    @GetMapping("/showAdd")
    public String addExpense(Model model) {
        model.addAttribute("expense", new ExpenseDTO());
        // 🔥 ADD THIS LINE
        model.addAttribute("categories", categoryService.findAll());
        return "add-expense";
    }

    @PostMapping("/submitAdd")
    public String submitAdd(@ModelAttribute("expense") ExpenseDTO expenseDTO, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        expenseDTO.setClientId(client.getId());
        expenseService.save(expenseDTO);
        return "redirect:/list";
    }

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        int clientId = client.getId();
        List<Expense> expenseList = expenseService.findAllExpensesByClientId(clientId);
        for (Expense expense : expenseList) {
            expense.setCategoryName(categoryService.findCategoryById(expense.getCategory().getId()).getName());
            expense.setDate(LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate().toString());
            expense.setTime(LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalTime().toString());
        }
        model.addAttribute("expenseList", expenseList);
        model.addAttribute("filter", new FilterDTO());
        return "list-page";
    }

    @GetMapping("/showUpdate")
    public String showUpdate(@RequestParam("expId") int id, Model model) {
        Expense expense = expenseService.findExpenseById(id);
        ExpenseDTO expenseDTO = new ExpenseDTO();
        expenseDTO.setAmount(expense.getAmount());
        expenseDTO.setCategory(expense.getCategory().getName());
        expenseDTO.setDescription(expense.getDescription());
        expenseDTO.setDateTime(expense.getDateTime());

        model.addAttribute("expense", expenseDTO);
        model.addAttribute("expenseId", id);
        return "update-page";
    }

    @PostMapping("/submitUpdate")
    public String update(@RequestParam("expId") int id, @ModelAttribute("expense") ExpenseDTO expenseDTO, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        expenseDTO.setExpenseId(id);
        expenseDTO.setClientId(client.getId());
        expenseService.update(expenseDTO);
        return "redirect:/list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("expId") int id) {
        expenseService.deleteExpenseById(id);
        return "redirect:/list";
    }


    @PostMapping("/processFilter")
    public String processFilter(@ModelAttribute("filter") FilterDTO filter, Model model) {
        System.out.println("--------------------------------------------------------------");
        System.out.println("filter values : " + filter);
        List<Expense> expenseList = expenseService.findFilterResult(filter);
        System.out.println("size ----> " + expenseList.size());
        System.out.println(expenseList);

        for (Expense expense : expenseList) {
            expense.setCategoryName(categoryService.findCategoryById(expense.getCategory().getId()).getName());
            expense.setDate(LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate().toString());
            expense.setTime(LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalTime().toString());
        }
        model.addAttribute("expenseList", expenseList);
        return "filter-result";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        Map<String, Integer> categoryData = expenseService.getCategorySummary(client.getId());
        Map<String, Integer> monthlyData = expenseService.getMonthlySummary(client.getId());

        model.addAttribute("categoryData", categoryData);
        model.addAttribute("monthlyData", monthlyData);
        model.addAttribute("highestCategory", expenseService.getHighestCategory(client.getId()));
        model.addAttribute("thisMonthExpense", expenseService.getThisMonthExpense(client.getId()));
        int thisMonth = expenseService.getThisMonthExpense(client.getId());
        int budget = client.getBudget();
        model.addAttribute("budget", budget);
        model.addAttribute("isOverBudget", thisMonth > budget);

        int income = client.getIncome();
        int expense = expenseService.getThisMonthExpense(client.getId());
        int savings = income - expense;
        model.addAttribute("income", income);
        model.addAttribute("savings", savings);
        Map<String, Integer> incomeData = new HashMap<>();

        // monthlyData ke same keys use karo
        for (String month : monthlyData.keySet()) {
            incomeData.put(month, client.getIncome());
        }
        model.addAttribute("incomeData", incomeData);

        double percent = 0;
        if (budget > 0) {
            percent = (thisMonth * 100.0) / budget;
        }

        model.addAttribute("percent", percent);

        // 🔥 EMAIL LOGIC
        if (thisMonth > budget && !client.isAlertSent()) {
            emailService.sendBudgetAlert(client.getEmail());
            smsService.sendSms(
                    client.getPhone(),
                    "Alert: You have exceeded your monthly budget!"
            );
            Client dbClient = clientService.findClientById(client.getId());
            dbClient.setAlertSent(true);
            clientService.saveClient(dbClient);
        }
        // 🔥 RESET ALERT
        if (thisMonth <= budget) {
            Client dbClient = clientService.findClientById(client.getId());
            dbClient.setAlertSent(false);
            clientService.saveClient(dbClient);
        }

        return "dashboard";
    }

    @GetMapping("/dashboard/filter")
    @ResponseBody
    public Map<String, Object> filterDashboard(@RequestParam String from, @RequestParam String to,
                                               HttpSession session) {
        Client client = (Client) session.getAttribute("client");

        Map<String, Object> result = new HashMap<>();
        result.put("categoryData", expenseService.getCategorySummaryByDate(client.getId(), from, to));
        result.put("monthlyData", expenseService.getMonthlySummaryByDate(client.getId(), from, to));
        result.put("thisMonthExpense", expenseService.getThisMonthExpense(client.getId()));
        result.put("highestCategory", expenseService.getHighestCategory(client.getId()));

        return result;
    }

    @GetMapping("/dashboard/export")
    public void exportExcel(@RequestParam String from, @RequestParam(required = false) String to,
                            HttpSession session, HttpServletResponse response) throws Exception {
        Client client = (Client) session.getAttribute("client");
        // ✅ if to empty → current date
        if (to == null || to.isEmpty()) {
            to = java.time.LocalDate.now().toString();
        }
        List<Expense> list = expenseService.findExpensesByDate(client.getId(), from, to);
        Workbook wb = new XSSFWorkbook();
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
            row.createCell(3).setCellValue(e.getDescription());
        }
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=expenses.xlsx");
        wb.write(response.getOutputStream());
        wb.close();
    }

    @GetMapping("/showContactPage")
    public String showContactPage(Model model) {
        model.addAttribute("contact", new ContactInfoDTO());
        return "contact-page";
    }

    @PostMapping("/saveContact")
    public String saveContact(@ModelAttribute("contact") ContactInfoDTO dto,
                              Authentication authentication) {
        String username = authentication.getName();
        contactService.updateContact(username, dto);
        return "contact-page";
    }

}
