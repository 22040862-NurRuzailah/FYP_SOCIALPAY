package com.example.demo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        // Fetch a transaction by paymentId
        Optional<Transaction> findByPaymentId(Long paymentId);

        // Fetch all flagged transactions
        List<Transaction> findByFlaggedTrue();

        // Fetch all manually flagged transactions
        List<Transaction> findByFlaggedTrueAndFlaggedAutomaticallyFalse();

        // Fetch all automatically flagged transactions
        List<Transaction> findByFlaggedTrueAndFlaggedAutomaticallyTrue();

        // Fetch all unflagged transactions
        List<Transaction> findByFlaggedFalse();

        // Fetch all transactions that have been flagged, either automatically or
        // manually
        List<Transaction> findByFlaggedTrueOrFlaggedAutomaticallyTrue();

        List<Transaction> findByUserIDAndTransactionDate(Long userId, LocalDate transactionDate);

        // Fetch transactions based on specific amount thresholds for flagging
        // automation
        List<Transaction> findByTransactionAmountGreaterThan(Double amount);

        List<Transaction> findAll(); // or other methods as needed

        @Query("SELECT t FROM Transaction t WHERE t.flagged = false GROUP BY t.userID, t.paymentId, t.transactionDate, t.transactionAmount, t.transactionType")
        List<Transaction> findDistinctTransactions();

        // Find potential duplicates by grouping on key transaction attributes
        @Query("SELECT t FROM Transaction t WHERE t.flagged = false AND t.id NOT IN (SELECT MIN(t2.id) FROM Transaction t2 GROUP BY t2.paymentId, t2.transactionDate, t2.transactionAmount)")
        List<Transaction> findDuplicateTransactions();

        // Fetch transactions by userId
        List<Transaction> findByUserID(Long userId);

        // Add a temporary method to log all transactions fetched
        default void logAllTransactions() {
                List<Transaction> allTransactions = findAll();
                allTransactions.forEach(transaction -> System.out.println("Transaction fetched: " + transaction));
        }

        @Query("SELECT t FROM Transaction t WHERE " +
                        "(:userId IS NULL OR t.userID = :userId) AND " +
                        "(:startDate IS NULL OR t.transactionDate >= :startDate) AND " +
                        "(:endDate IS NULL OR t.transactionDate <= :endDate) AND " +
                        "(:year IS NULL OR YEAR(t.transactionDate) = :year) AND " +
                        "(:month IS NULL OR MONTH(t.transactionDate) = :month) AND " +
                        "(:amount IS NULL OR (:amountOperator = '>' AND t.transactionAmount > :amount) OR (:amountOperator = '<' AND t.transactionAmount < :amount))")
        List<Transaction> findFilteredTransactions(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("year") Integer year,
                        @Param("month") Integer month,
                        @Param("amount") Double amount,
                        @Param("amountOperator") String amountOperator);

        @Query("SELECT t FROM Transaction t WHERE " +
                        "t.flagged = true AND " +
                        "t.flaggedAutomatically = false AND " +
                        "(:startDate IS NULL OR t.transactionDate >= :startDate) AND " +
                        "(:endDate IS NULL OR t.transactionDate <= :endDate) AND " +
                        "(:amount IS NULL OR " +
                        "(:amountOperator = '>' AND t.transactionAmount > :amount) OR " +
                        "(:amountOperator = '<' AND t.transactionAmount < :amount))")
        List<Transaction> findFlaggedTransactions(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("amount") Double amount,
                        @Param("amountOperator") String amountOperator);

}