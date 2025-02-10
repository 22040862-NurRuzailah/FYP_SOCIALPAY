package c300.ruzailah.fyp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SignupController {

    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup"; // The name of your signup.html file (ensure it's in the templates folder)
    }
}
