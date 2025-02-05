package c300.ruzailah.fyp;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

@Controller
public class AdminController {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/admin-landing")
    public String adminLandingPage(Model model) {
        List<Member> bannedMembers = memberRepository.findAll().stream()
                .filter(m -> !m.isEnabled())
                .collect(Collectors.toList());

        List<Transaction> flaggedTxns = transactionRepository.findAll().stream()
                .filter(t -> t.isFlagged() && !t.isFlaggedAutomatically())
                .collect(Collectors.toList());
        List<Transaction> systemFlaggedTxns = transactionRepository.findAll().stream()
                .filter(t -> t.isFlagged() && t.isFlaggedAutomatically())
                .collect(Collectors.toList());
        List<Post> reportedPosts = postRepository.findAll().stream()
                .filter(Post::isReported)
                .collect(Collectors.toList());

        int bannedMembersCount = bannedMembers.size();
        int flaggedTxnsCount = flaggedTxns.size();
        int systemFlaggedTxnsCount = systemFlaggedTxns.size();
        int reportedPostsCount = reportedPosts.size();

        System.out.println("Banned Members Count: " + bannedMembersCount);
        System.out.println("Flagged Txns Count: " + flaggedTxnsCount);
        System.out.println("System Flagged Txns Count: " + systemFlaggedTxnsCount);
        System.out.println("Reported Posts Count: " + reportedPostsCount);

        model.addAttribute("bannedMembersCount", bannedMembersCount);
        model.addAttribute("flaggedTxnsCount", flaggedTxnsCount);
        model.addAttribute("systemFlaggedTxnsCount", systemFlaggedTxnsCount);
        model.addAttribute("reportedPostsCount", reportedPostsCount);

        return "admin-landing";
    }

    @GetMapping("/manage-admin")
    public String manageAdminPage(Principal principal, Model model) {
        List<Member> adminList = memberRepository.findByRole("ROLE_ADMIN");
        List<Member> admins = new ArrayList<>();
        for (Member admin : adminList) {
            if (!admin.getEmail().equals(principal.getName())) {
                admins.add(admin);
            }
        }
        model.addAttribute("admins", admins);
        return "manage-admin";
    }

    @GetMapping("/admin-txn")
    public String getLogHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String amountOperator, // New parameter
            Model model) {

        List<Transaction> transactions = transactionService.getFilteredTransactions(userId, startDate, endDate, year,
                month, amount, amountOperator);

        model.addAttribute("transactions", transactions);
        return "admin-txn";
    }

    @GetMapping("/admin-flagged-txn")
    public String getFlaggedTransactions(
            @RequestParam(required = false) Boolean flaggedAutomatically,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String amountOperator,
            Model model) {

        List<Transaction> flaggedTransactions = transactionService.getFilteredFlaggedTransactions(
                flaggedAutomatically, startDate, endDate, amount, amountOperator);

        model.addAttribute("transactions", flaggedTransactions);
        return "admin-flagged-txn"; // New Thymeleaf template for flagged transactions
    }

    @PostMapping("/flag-transaction")
    @ResponseBody
    public ResponseEntity<Map<String, String>> flagTransaction(@RequestBody Map<String, Long> requestBody) {
        try {
            Long paymentId = requestBody.get("paymentId");
            transactionService.flagTransaction(paymentId);
            return ResponseEntity.ok(Map.of("message", "Transaction successfully flagged."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to flag the transaction: " + e.getMessage()));
        }
    }

    @PostMapping("/unflag-transaction")
    @ResponseBody
    public ResponseEntity<Map<String, String>> unflagTransaction(@RequestBody Map<String, Long> requestBody) {
        try {
            Long paymentId = requestBody.get("paymentId");
            transactionService.unflagTransaction(paymentId);
            return ResponseEntity.ok(Map.of("message", "Transaction successfully unflagged."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unflag the transaction: " + e.getMessage()));
        }
    }

    // Endpoint to handle duplicate transactions manually
    @PostMapping("/handle-duplicates")
    @ResponseBody
    public ResponseEntity<String> handleDuplicates() {
        try {
            System.out.println("Manual request: Handling duplicate transactions.");
            transactionService.handleDuplicateTransactions();
            return ResponseEntity.ok("Duplicates have been handled successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to handle duplicate transactions: " + e.getMessage());
        }
    }

    public void automateFlaggingForAllTransactions() {
        System.out.println("Scheduled task: Automating suspicious transaction flagging.");
        List<Transaction> transactions = transactionService.getAllTransactions();
        transactions.forEach(transactionService::automateFlagging);
    }

    @Scheduled(cron = "0 0 2 * * *") // Runs daily at 2 AM
    public void scheduledHandleDuplicates() {
        System.out.println("Scheduled task: Handling duplicate transactions.");
        transactionService.handleDuplicateTransactions();
    }

    @PostMapping("/trigger-handle-duplicates")
    @ResponseBody
    public ResponseEntity<String> triggerHandleDuplicates() {
        try {
            System.out.println("Manual trigger: Handling duplicate transactions.");
            transactionService.handleDuplicateTransactions();
            return ResponseEntity.ok("Duplicate handling triggered manually.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to handle duplicate transactions: " + e.getMessage());
        }
    }

}