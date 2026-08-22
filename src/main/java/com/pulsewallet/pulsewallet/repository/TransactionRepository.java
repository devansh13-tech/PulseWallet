package com.pulsewallet.pulsewallet.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.entity.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndTransactionDateBetween(
            Long userId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Ownership check and lookup in one query - a wrong-owner id simply isn't
     * found.
     */
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.user.id = :userId and t.type = :type
              and t.transactionDate between :from and :to
            """)
    java.math.BigDecimal sumAmountByUserIdAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select t.transactionDate as period, sum(t.amount) as total
            from Transaction t
            where t.user.id = :userId and t.type = :type
              and t.transactionDate between :from and :to
            group by t.transactionDate order by t.transactionDate
            """)
    java.util.List<PeriodTotalProjection> sumByPeriod(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select c.id as categoryId, c.name as categoryName, sum(t.amount) as total
            from Transaction t left join t.category c
            where t.user.id = :userId and t.type = com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE
              and t.transactionDate between :from and :to
            group by c.id, c.name order by sum(t.amount) desc
            """)
    java.util.List<CategoryTotalProjection> sumExpensesByCategory(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select year(t.transactionDate) as year, month(t.transactionDate) as month, sum(t.amount) as total
            from Transaction t
            where t.user.id = :userId and t.transactionDate between :from and :to
            group by year(t.transactionDate), month(t.transactionDate)
            order by year(t.transactionDate), month(t.transactionDate)
            """)
    java.util.List<MonthlyTotalProjection> sumByMonth(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select year(t.transactionDate) as year, month(t.transactionDate) as month,
               c.id as categoryId, c.name as categoryName, sum(t.amount) as total
            from Transaction t left join t.category c
            where t.user.id = :userId and t.type = com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE
              and t.transactionDate between :from and :to
            group by year(t.transactionDate), month(t.transactionDate), c.id, c.name
            order by year(t.transactionDate), month(t.transactionDate), sum(t.amount) desc
            """)
    java.util.List<CategoryMonthlyTotalProjection> sumExpensesByCategoryAndMonth(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
