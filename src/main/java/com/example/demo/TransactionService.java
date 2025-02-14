package com.example.demo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MemberRepository memberRepository;

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getTransactionsSortedByAmountDesc() {
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "transactionAmount"));
    }

    public List<Transaction> getTransactionsSortedByAmountAsc() {
        return transactionRepository.findAll(Sort.by(Sort.Direction.ASC, "transactionAmount"));
    }

    public List<Transaction> getTransactionsSortedByUserIdAsc() {
        return transactionRepository.findAll(Sort.by(Sort.Direction.ASC, "userID"));
    }

    public List<Transaction> getTransactionsSortedByUserIdDesc() {
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "userID"));
    }

    public List<Transaction> getDistinctTransactions() {
        return transactionRepository.findDistinctTransactions();
    }

    public List<Transaction> getFilteredFlaggedTransactions(
        Boolean flaggedAutomatically, LocalDate startDate, LocalDate endDate, Double amount, String amountOperator) {
    return transactionRepository.findFlaggedTransactions(startDate, endDate, amount, amountOperator);
}

    public List<Transaction> getFilteredTransactions(Long userId, LocalDate startDate, LocalDate endDate, Integer year, Integer month, Double amount, String amountOperator) {
        return transactionRepository.findFilteredTransactions(userId, startDate, endDate, year, month, amount, amountOperator);
    }

    public void clearFlag(Long paymentId) {
        Transaction transaction = findTransactionByPaymentId(paymentId);
        transaction.setFlagged(false);
        transaction.setFlaggedAutomatically(false);
        transactionRepository.save(transaction);
    }

    public void flagTransaction(Long paymentId, String reason) {
        Transaction transaction = transactionRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setFlagged(true);
        transaction.setFlaggedAutomatically(false);
        transaction.setFlagReason(reason);
        transaction.setCleared(false);
        transactionRepository.save(transaction);

        Notification notification = new Notification();
        notification.setMessage("Transaction ID " + paymentId + " flagged: " + reason);
        notificationRepository.save(notification);
    }

    public void unflagTransaction(Long paymentId) {
        Transaction transaction = transactionRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setFlagged(false);
        transaction.setFlaggedAutomatically(false);
        transaction.setFlagReason(null);
        transaction.setCleared(true);
        transactionRepository.save(transaction);
    }

    public void addTransaction(Transaction transaction) {
        System.out.println("Saving transaction to DB: " + transaction); // Add this line
        transactionRepository.save(transaction);
        System.out.println("Transaction saved successfully"); // Add this line
        detectAndFlagSuspiciousTransaction(transaction);
       
    }
    

    public void detectAndFlagSuspiciousTransaction(Transaction transaction) {
        System.out.println("Transaction amount: " + transaction.getTransactionAmount());

        List<Transaction> userTransactions = transactionRepository.findByUserID(transaction.getUserID());

        if (!userTransactions.isEmpty()) {
            double avg = userTransactions.stream()
                    .mapToDouble(Transaction::getTransactionAmount)
                    .average()
                    .orElse(0.0);
            double upperThreshold = avg * 1.50; // 50% above average

            if (transaction.getTransactionAmount() > upperThreshold && !transaction.getFlagged() && !transaction.isCleared()) {
                transaction.setFlagged(true);
                transaction.setFlaggedAutomatically(true);
                transaction.setFlagReason("Suspicious transaction amount");
                transaction.setCleared(false);
                transactionRepository.save(transaction);
                Notification notification = new Notification();
                notification.setMessage("Suspicious transaction detected: " + transaction.getTransactionAmount() + " for user ID: " + transaction.getUserID());
                notificationRepository.save(notification);
            }

            else if (transaction.getIpAddress() != null && !check_ip_origin(transaction.getIpAddress()) && !transaction.isCleared()) {
                transaction.setFlagged(true);
                transaction.setFlaggedAutomatically(true);
                transaction.setFlagReason("Suspicious IP address");
                transaction.setCleared(false);
                transactionRepository.save(transaction);
                Notification notification = new Notification();
                notification.setMessage("Suspicious IP address detected: " + transaction.getIpAddress() + " for user ID: " + transaction.getUserID());
                notificationRepository.save(notification);
            }

            // else if (!checkTransactionFrequency(transaction)) {
            //     transaction.setFlagged(true);
            //     transaction.setFlaggedAutomatically(true);
            //     transaction.setFlagReason("Suspicious transaction frequency");
            //     transactionRepository.save(transaction);
            // }
        }
    }

    private boolean checkTransactionFrequency(Transaction transaction) {
    List<Transaction> allUserTransactions = transactionRepository.findByUserID(transaction.getUserID());
    List<Transaction> specificDateTransactions = transactionRepository.findByUserIDAndTransactionDate(
            transaction.getUserID(),
            transaction.getTransactionDate());
    
    Map<LocalDate, Integer> transactionsPerDay = new HashMap<>();
    
    for (Transaction t : allUserTransactions) {
        transactionsPerDay.merge(t.getTransactionDate(), 1, Integer::sum);
    }
    
    double totalTransactions = transactionsPerDay.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    double averageTransactionsPerDay = totalTransactions / transactionsPerDay.size();
    
    double sumSquaredDifferences = transactionsPerDay.values().stream()
            .mapToDouble(count -> Math.pow(count - averageTransactionsPerDay, 2))
            .sum();
    double standardDeviation = Math.sqrt(sumSquaredDifferences / transactionsPerDay.size());
    
    double threshold = averageTransactionsPerDay + (2 * standardDeviation);
    
    int currentDayCount = specificDateTransactions.size() + 1;
    
    return currentDayCount <= threshold;
}


    private boolean check_ip_origin(String ipAddress) {
        try {
            long ip = 0, startIp = 0, endIp = 0;

            for (byte b : InetAddress.getByName(ipAddress).getAddress()) {
                ip = (ip << 8) | (b & 0xFF);
            }
            for (byte b : InetAddress.getByName("118.189.0.0").getAddress()) {
                startIp = (startIp << 8) | (b & 0xFF);
            }
            for (byte b : InetAddress.getByName("118.189.255.255").getAddress()) {
                endIp = (endIp << 8) | (b & 0xFF);
            }

            return ip >= startIp && ip <= endIp;
        } catch (Exception e) {
            return false;
        }
    }

    private Transaction findTransactionByPaymentId(Long paymentId) {
        return transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for paymentId: " + paymentId));
    }

    @CacheEvict(value = { "transactionsCache", "flaggedTransactionsCache",
            "distinctTransactionsCache" }, allEntries = true)
    public void automateFlagging(Transaction transaction) {
        detectAndFlagSuspiciousTransaction(transaction);
    }



    // Handle duplicate transactions: flag duplicates
    @CacheEvict(value = { "transactionsCache", "distinctTransactionsCache",
            "flaggedTransactionsCache" }, allEntries = true)
    public void handleDuplicateTransactions() {
        System.out.println("handleDuplicateTransactions() invoked");
        List<Transaction> allTransactions = transactionRepository.findAll();

        // Log all fetched transactions for verification
        allTransactions.forEach(transaction -> System.out.println("Transaction fetched: " + transaction));

        Map<String, List<Transaction>> groupedTransactions = allTransactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getPaymentId() + "-" +
                                transaction.getTransactionDate() + "-" +
                                transaction.getTransactionAmount()));

        System.out.println("Processing grouped transactions:");
        groupedTransactions.forEach((key, list) -> {
            System.out.println("Group Key: " + key + ", Transactions: " + list.size());
            if (list.size() > 1) {
                for (int i = 1; i < list.size(); i++) { // Flag all but the first entry
                    Transaction duplicate = list.get(i);
                    System.out.println("Flagging duplicate transaction: " + duplicate);
                    duplicate.setFlagged(true);
                    duplicate.setFlaggedAutomatically(true);
                    transactionRepository.save(duplicate);
                    // Ensure the changes are flushed to the database
                    transactionRepository.flush();
                }
            }
        });
    }

    @Scheduled(cron = "0 */5 * * * *") 
    public void systemAutomatedDetection() {
        List<Transaction> transactions = transactionRepository.findAll();
        for (Transaction transaction : transactions) {
            detectAndFlagSuspiciousTransaction(transaction);
        }
    }

    // Scheduled task to handle duplicate transactions automatically
    @Scheduled(cron = "0 0 2 * * *") // Runs daily at 2 AM
    public void scheduledHandleDuplicates() {
        System.out.println("Scheduled task: Handling duplicate transactions");
        handleDuplicateTransactions();
    }


    @Scheduled(cron = "0 */5 * * * *") 
    public void banUser() {
        List<Member> members = memberRepository.findAll();

        for (Member member : members) {
            List<Transaction> flaggedTransactions = transactionRepository.findByUserID(member.getId())
                    .stream()
                    .filter(Transaction::isFlagged)
                    .collect(Collectors.toList());

            if (flaggedTransactions.size() > 3 && member.isEnabled()) {
                member.setEnabled(false);
                memberRepository.save(member);
                System.out.println("User " + member.getId() + " has been banned.");
                Notification notification = new Notification();
                notification.setMessage("Account ID "+ member.getId() +" has been disabled due to multiple flagged transactions.");
                notificationRepository.save(notification);
            }
        }
    }
}
