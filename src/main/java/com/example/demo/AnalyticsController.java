package com.example.demo;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class AnalyticsController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @GetMapping("/analytics")
    public String userAnalyticCharts(Model model, Principal principal) {
        Member member = memberRepository.findByEmail(principal.getName()).get();
    
        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getUserID() == member.getId())
                .collect(Collectors.toList());
                
        model.addAttribute("transactions", transactions);
        return "analytics";
    }
}
