package com.example.demo;

import java.security.Principal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MemberController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;


    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Incorrect current password.");
            return "redirect:/socialfeed";
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        return "redirect:/login";
    }

    @PostMapping("/change-email")
    public String changeEmail
            (@RequestParam("newEmail") String newEmail, Principal principal, RedirectAttributes redirectAttributes) {
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (memberRepository.findByEmail(newEmail).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email is already registered.");
            return "redirect:/socialfeed";
        }

        member.setEmail(newEmail);
        memberRepository.save(member);

        redirectAttributes.addFlashAttribute("success", "Email changed successfully.");
        return "redirect:/login";
    }
    
    @PostMapping("/signup")
    public String registerUser(@ModelAttribute("member") Member member, RedirectAttributes redirectAttributes) {
        if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email is already registered!");
            return "redirect:/signup";
        }

        // Hash the password
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        member.setRole("ROLE_USER");
        member.setEnabled(false); // Account is disabled until OTP is verified

        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(999999)); // Generate 6-digit OTP
        member.setOtp(otp);

        // Set OTP expiry time (5 minutes from now)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5); // Add 5 minutes
        member.setOtpExpiryTime(calendar.getTime());

        memberRepository.save(member);

        // Send OTP via email
        emailService.sendOtpEmail(member.getEmail(), member.getName(), otp);

        redirectAttributes.addFlashAttribute("success", "Sign-up successful! Check your email for the OTP to activate your account.");
        return "redirect:/verify-otp?email=" + member.getEmail(); // Redirect to OTP verification page
    }

    @GetMapping("/verify-otp")
    public String showOtpPage(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "otp-verification"; // Render the OTP input page
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        Optional<Member> memberOptional = memberRepository.findByEmail(email);
        if (memberOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email.");
        }

        Member member = memberOptional.get();
        if (member.getOtp() == null || !member.getOtp().equals(otp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP.");
        }

        if (new Date().after(member.getOtpExpiryTime())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP has expired.");
        }

        // Activate account
        member.setEnabled(true);
        member.setOtp(null);
        member.setOtpExpiryTime(null);
        memberRepository.save(member);

        return ResponseEntity.ok("Account activated successfully.");
    }


    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        Optional<Member> memberOptional = memberRepository.findByEmail(email);
        if (memberOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid email.");
            return "redirect:/verify-otp?email=" + email;
        }

        Member member = memberOptional.get();
        String newOtp = String.format("%06d", new Random().nextInt(999999));
        member.setOtp(newOtp);

        // Set new OTP expiry time (5 minutes from now)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5); // Add 5 minutes
        member.setOtpExpiryTime(calendar.getTime());

        memberRepository.save(member);

        emailService.sendOtpEmail(member.getEmail(), member.getName(), newOtp);

        redirectAttributes.addFlashAttribute("success", "A new OTP has been sent to your email.");
        return "redirect:/verify-otp?email=" + email;
    }

    @PostMapping("/addAdmin")
    public String addAdmin(@ModelAttribute("member") Member member) {
        if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
            return "redirect:/admin-landing";
        }

        // Hash the password
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        member.setRole("ROLE_ADMIN");
        member.setEnabled(true);

        memberRepository.save(member);

        return "redirect:/manage-admin";
    }


    @PostMapping("deleteAdmin")
    public String deleteAdminr(@RequestParam("id") Long id) {
        memberRepository.deleteById(id);
        return "redirect:/manage-admin";

    }

    @PostMapping("/linkPayPal")
    public String linkPayPal( Principal principal, @RequestParam("email") String email) {
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        member.setPaypalAccount(email); 
        memberRepository.save(member);
        return "redirect:/socialfeed";
    }

    @PostMapping("/unban/{id}")
    public String unbanMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        for (Transaction transaction : transactionRepository.findByUserID(member.getId())) {
            transaction.setCleared(true);
            transaction.setFlagReason(null);
            transaction.setFlagged(false);
            transaction.setFlaggedAutomatically(false);
            transactionRepository.save(transaction);
        }

        member.setEnabled(true);
        memberRepository.save(member);
        return "redirect:/admin-landing";

    }


    @GetMapping("/user-transaction")
    public String showTransactionPage(Principal principal, Model model) {
        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Transaction> transactions = transactionRepository.findByUserID(member.getId());

        model.addAttribute("transactions", transactions);
        return "user-transaction";

    }

}
