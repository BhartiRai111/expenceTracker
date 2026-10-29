package com.SpringBootMVC.ExpensesTracker.repository;

import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Integer> {
    List<Expense> findByClientId(int id);

    Optional<Expense> findByIdAndClientId(int id, int clientId);

    @Query("select e.category.name, sum(e.amount) from Expense e " +
            "where e.client.id = :clientId group by e.category.name")
    List<Object[]> sumByCategory(@Param("clientId") int clientId);

    @Query("select e.category.name, sum(e.amount) from Expense e " +
            "where e.client.id = :clientId and substring(e.dateTime, 1, 10) between :from and :to " +
            "group by e.category.name")
    List<Object[]> sumByCategoryBetween(@Param("clientId") int clientId,
                                        @Param("from") String from, @Param("to") String to);

    @Query("select substring(e.dateTime, 1, 7), sum(e.amount) from Expense e " +
            "where e.client.id = :clientId group by substring(e.dateTime, 1, 7)")
    List<Object[]> sumByMonth(@Param("clientId") int clientId);

    @Query("select substring(e.dateTime, 1, 7), sum(e.amount) from Expense e " +
            "where e.client.id = :clientId and substring(e.dateTime, 1, 10) between :from and :to " +
            "group by substring(e.dateTime, 1, 7)")
    List<Object[]> sumByMonthBetween(@Param("clientId") int clientId,
                                     @Param("from") String from, @Param("to") String to);

    @Query("select coalesce(sum(e.amount), 0) from Expense e " +
            "where e.client.id = :clientId and substring(e.dateTime, 1, 7) = :month")
    long sumForMonth(@Param("clientId") int clientId, @Param("month") String month);

    @Query("select e from Expense e where e.client.id = :clientId " +
            "and substring(e.dateTime, 1, 10) between :from and :to order by e.dateTime")
    List<Expense> findBetween(@Param("clientId") int clientId,
                              @Param("from") String from, @Param("to") String to);

    @Query("select e from Expense e where e.client.id = :clientId " +
            "and (:categoryId is null or e.category.id = :categoryId) " +
            "and e.amount between :minAmount and :maxAmount " +
            "and (:year is null or substring(e.dateTime, 1, 4) = :year) " +
            "and (:month is null or substring(e.dateTime, 6, 2) = :month) " +
            "order by e.dateTime desc")
    List<Expense> filter(@Param("clientId") int clientId, @Param("categoryId") Integer categoryId,
                         @Param("minAmount") int minAmount, @Param("maxAmount") int maxAmount,
                         @Param("year") String year, @Param("month") String month);
}
