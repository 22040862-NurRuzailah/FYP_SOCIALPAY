package c300.ruzailah.fyp;

import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.mail.internet.MimeMessage;

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

    @Autowired
    private ReportedPostsRepository reportedPostsRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/admin-landing")
    public String adminLandingPage(Model model) {
        List<Member> bannedMembers = memberRepository.findAll().stream()
                .filter(m -> !m.isEnabled())
                .collect(Collectors.toList());

        List<Transaction> flaggedTxns = transactionRepository.findAll().stream()
                .filter(t -> t.isFlagged() && !t.isFlaggedAutomatically() && !t.isReported())
                .collect(Collectors.toList());
        List<Transaction> systemFlaggedTxns = transactionRepository.findAll().stream()
                .filter(t -> t.isFlagged() && t.isFlaggedAutomatically())
                .collect(Collectors.toList());
        List<ReportedPosts> reportedPosts = reportedPostsRepository.findAll();

        List<Post> posts = postRepository.findAll().stream()
                .filter(p -> p.getReportedCount() > 0)
                .collect(Collectors.toList());

        int bannedMembersCount = bannedMembers.size();
        int flaggedTxnsCount = flaggedTxns.size();
        int systemFlaggedTxnsCount = systemFlaggedTxns.size();
        int reportedPostsCount = reportedPosts.size();

        model.addAttribute("bannedMembers", bannedMembers);
        model.addAttribute("flaggedTxns", flaggedTxns);
        model.addAttribute("systemFlaggedTxns", systemFlaggedTxns);
        model.addAttribute("reportedPosts", posts);

        model.addAttribute("bannedMembersCount", bannedMembersCount);
        model.addAttribute("flaggedTxnsCount", flaggedTxnsCount);
        model.addAttribute("systemFlaggedTxnsCount", systemFlaggedTxnsCount);
        model.addAttribute("reportedPostsCount", reportedPostsCount);

        model.addAttribute("notifications", notificationRepository.findAll());

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
    public ResponseEntity<Map<String, String>> flagTransaction(@RequestBody Map<String, Object> requestBody) {
        try {
            Long paymentId = Long.parseLong(requestBody.get("paymentId").toString());
            String flagReason = requestBody.get("flagReason").toString();

            transactionService.flagTransaction(paymentId, flagReason);
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

    @PostMapping("/notification/{id}/delete")
    public String deleteNotification(@PathVariable Long id) {
        notificationRepository.deleteById(id);
        return "redirect:/admin-landing";
    }

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/send-pdf-report")
    @ResponseBody
    public ResponseEntity<String> sendPdfReport(@RequestParam("txnID") Long txnID) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("socialpayteam00@gmail.com");
            helper.setTo("socialpayteam00@gmail.com");
            helper.setSubject("Suspicious Transaction Alert - Action Required");

           
            Transaction txn = transactionRepository.findById(txnID).orElse(null);
            Member sender = memberRepository.findById(txn.getUserID()).orElse(null);

            helper.setText("Dear Admin Team,\n\n" +
                    "This email is to bring to your attention a flagged transaction that requires your immediate review.\n\n"
                    +
                    "Transaction Details:\n" +
                    "- Transaction ID: " + txn.getPaymentId() + "\n" +
                    "- User ID: " + sender.getId() + "\n" +
                    "- User Name: " + sender.getName() + "\n" +
                    "- User Email: " + sender.getEmail() + "\n" +
                    "- Transaction Amount: $" + txn.getTransactionAmount() + "\n" +
                    "- Transaction Date: " + txn.getTransactionDate() + "\n" +
                    "- Flag Reason: " + txn.getFlagReason() + "\n\n" +
                    "Please find attached the detailed transaction report for your investigation.\n\n" +
                    "Best regards,\n" +
                    "SocialPay Security Team");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            String logoPath = "src/main/resources/static/images/SPF.png";
            Image img = Image.getInstance(logoPath);
            img.setAlignment(Element.ALIGN_CENTER);
            img.scaleToFit(200, 100);
            document.add(img);

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("TRANSACTION INVESTIGATION REPORT"));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Transaction Details:"));
            document.add(new Paragraph("Transaction ID: " + txn.getPaymentId()));
            document.add(new Paragraph("Amount: $" + txn.getTransactionAmount()));
            document.add(new Paragraph("Date: " + txn.getTransactionDate()));
            document.add(new Paragraph("Flag Reason: " + txn.getFlagReason()));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("User Information:"));
            document.add(new Paragraph("User ID: " + sender.getId()));
            document.add(new Paragraph("Name: " + sender.getName()));
            document.add(new Paragraph("Email: " + sender.getEmail()));

            document.close();

            byte[] pdfBytes = outputStream.toByteArray();
            helper.addAttachment("TransactionReport.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);

            txn.setReported(true);
            transactionRepository.save(txn);

            return ResponseEntity.ok("PDF report sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send PDF report: " + e.getMessage());
        }
    }

}