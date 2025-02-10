package c300.ruzailah.fyp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

	@GetMapping("/login")
	public String login() {
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
