package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
public class LoginController {

	  @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        }
        return "login"; 
    }

    @PostMapping("/login")
    public String handleLogin() {
        // Redirect to social feed after successful login
        return "redirect:/socialfeed";
    }

//    @GetMapping("/socialfeed")
//    public String showSocialFeed() {
//        return "socialfeed"; // This will serve the socialfeed.html page
//    }
}
