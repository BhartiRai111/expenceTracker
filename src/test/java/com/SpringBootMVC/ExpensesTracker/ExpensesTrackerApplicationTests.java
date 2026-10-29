package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.DTO.CustomUserDetails;
import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.repository.CategoryRepository;
import com.SpringBootMVC.ExpensesTracker.repository.ClientRepository;
import com.SpringBootMVC.ExpensesTracker.repository.ExpenseRepository;
import com.SpringBootMVC.ExpensesTracker.service.AlertService;
import com.SpringBootMVC.ExpensesTracker.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExpensesTrackerApplicationTests {

    @Autowired ExpenseService expenseService;
    @Autowired ExpenseRepository expenseRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired MockMvc mockMvc;
    @MockBean AlertService alertService;

    Client alice;
    Client bob;
    String thisMonth = LocalDate.now().toString().substring(0, 7);

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        clientRepository.deleteAll();
        categoryRepository.deleteAll();
        categoryRepository.save(new Category(1, "Groceries"));
        categoryRepository.save(new Category(2, "Travel"));
        alice = clientRepository.save(new Client("Alice", "A", "a@x.com", 1000, 100));
        bob = clientRepository.save(new Client("Bob", "B", "b@x.com", 1000, 100));
        reset(alertService);
    }

    private ExpenseDTO dto(Client client, String category, int amount, String dateTime) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setClientId(client.getId());
        dto.setCategory(category);
        dto.setAmount(amount);
        dto.setDateTime(dateTime);
        dto.setDescription("test");
        return dto;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void summariesAreScopedAndAggregatedInSql() {
        expenseService.save(dto(alice, "Groceries", 10, thisMonth + "-01T10:00"));
        expenseService.save(dto(alice, "Groceries", 5, thisMonth + "-02T10:00"));
        expenseService.save(dto(alice, "Travel", 7, "2020-01-05T10:00"));
        expenseService.save(dto(bob, "Travel", 99, thisMonth + "-03T10:00"));

        assertEquals(15, expenseService.getCategorySummary(alice.getId()).get("Groceries"));
        assertEquals(7, expenseService.getCategorySummary(alice.getId()).get("Travel"));
        assertEquals(15, expenseService.getThisMonthExpense(alice.getId()));
        assertEquals("Groceries", expenseService.getHighestCategory(alice.getId()));
        assertEquals(1, expenseService.getCategorySummaryByDate(alice.getId(), "2020-01-01", "2020-12-31").size());
        assertEquals(7, expenseService.getMonthlySummaryByDate(alice.getId(), "2020-01-01", "2020-12-31").get("2020-01"));
        assertEquals(3, expenseService.findExpensesByDate(alice.getId(), "2000-01-01", "2999-01-01").size());
    }

    @Test
    void otherUsersExpensesCannotBeReadChangedOrDeleted() {
        expenseService.save(dto(bob, "Travel", 20, thisMonth + "-03T10:00"));
        Expense bobs = expenseRepository.findByClientId(bob.getId()).get(0);

        assertThrows(ResponseStatusException.class, () -> expenseService.findExpenseById(bobs.getId(), alice.getId()));
        assertThrows(ResponseStatusException.class, () -> expenseService.deleteExpenseById(bobs.getId(), alice.getId()));
        ExpenseDTO hijack = dto(alice, "Travel", 1, thisMonth + "-03T10:00");
        hijack.setExpenseId(bobs.getId());
        assertThrows(ResponseStatusException.class, () -> expenseService.update(hijack));
        assertEquals(1, expenseRepository.findByClientId(bob.getId()).size());
    }

    @Test
    void filterOnlyReturnsOwnExpensesAndHandlesAll() {
        expenseService.save(dto(alice, "Groceries", 10, "2024-03-01T10:00"));
        expenseService.save(dto(alice, "Travel", 50, "2024-04-01T10:00"));
        expenseService.save(dto(bob, "Travel", 50, "2024-04-01T10:00"));

        FilterDTO filter = new FilterDTO();
        filter.setCategory("all");
        filter.setYear("all");
        filter.setMonth("all");
        filter.setFrom(0);
        filter.setTo(1000);
        assertEquals(2, expenseService.findFilterResult(alice.getId(), filter).size());

        filter.setCategory("Travel");
        filter.setYear("2024");
        filter.setMonth("4");
        assertEquals(1, expenseService.findFilterResult(alice.getId(), filter).size());

        filter.setMonth("4; drop table expense");
        assertThrows(ResponseStatusException.class, () -> expenseService.findFilterResult(alice.getId(), filter));
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(ResponseStatusException.class,
                () -> expenseService.save(dto(alice, "NoSuchCategory", 1, "2024-03-01T10:00")));
        assertThrows(ResponseStatusException.class,
                () -> expenseService.save(dto(alice, "Travel", 1, "not-a-date")));
        assertThrows(ResponseStatusException.class,
                () -> expenseService.getCategorySummaryByDate(alice.getId(), "bad", "2024-01-01"));
    }

    @Test
    void budgetAlertFiresOnceAndRearmsWhenSpendDropsBack() {
        expenseService.save(dto(alice, "Groceries", 150, thisMonth + "-01T10:00"));
        verify(alertService, times(1)).sendAlert(any(Client.class), anyInt());
        assertTrue(clientRepository.findById(alice.getId()).orElseThrow().isAlertSent());

        expenseService.save(dto(alice, "Groceries", 10, thisMonth + "-02T10:00"));
        verify(alertService, times(1)).sendAlert(any(Client.class), anyInt());

        for (Expense e : expenseRepository.findByClientId(alice.getId())) {
            expenseService.deleteExpenseById(e.getId(), alice.getId());
        }
        assertFalse(clientRepository.findById(alice.getId()).orElseThrow().isAlertSent());
    }

    @Test
    void anonymousUsersCannotUseApiOrMutations() throws Exception {
        mockMvc.perform(post("/api/chat").with(csrf()).contentType("application/json").content("{\"message\":\"hi\"}"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/delete").param("expId", "1")).andExpect(status().is3xxRedirection());
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        CustomUserDetails principal = new CustomUserDetails("alice", "x",
                List.of(new SimpleGrantedAuthority("ROLE_STANDARD")), alice.getId());
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        mockMvc.perform(post("/delete").param("expId", "1").with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingSomeoneElsesExpenseOverHttpReturnsNotFound() throws Exception {
        expenseService.save(dto(bob, "Travel", 20, thisMonth + "-03T10:00"));
        int bobsExpense = expenseRepository.findByClientId(bob.getId()).get(0).getId();
        CustomUserDetails principal = new CustomUserDetails("alice", "x",
                List.of(new SimpleGrantedAuthority("ROLE_STANDARD")), alice.getId());
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        mockMvc.perform(post("/delete").param("expId", String.valueOf(bobsExpense))
                        .with(authentication(auth)).with(csrf()))
                .andExpect(status().isNotFound());
        assertEquals(1, expenseRepository.findByClientId(bob.getId()).size());
    }
}
