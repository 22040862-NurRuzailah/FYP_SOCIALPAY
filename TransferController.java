package c300.ruzailah.fyp;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
@RestController
@RequestMapping("/transfer")
public class TransferController {

    @Autowired
    private PayPalService payPalService;
    
    @Autowired
    private MemberRepository memberRepository;

    // Change to @Controller to handle view resolution
    @GetMapping("/success")
    public ModelAndView paymentSuccess(
            @RequestParam("paymentId") String paymentId, 
            @RequestParam("PayerID") String payerId) {
        
        ModelAndView modelAndView = new ModelAndView("success"); // Points to transfer-success.html
        
        try {
            // Execute the payment
            payPalService.executePayment(paymentId, payerId);

            // Fetch payment details
            JSONObject paymentDetails = payPalService.getPaymentDetails(paymentId);
            
            // Add all necessary attributes to the model
            modelAndView.addObject("paymentId", paymentId);
            modelAndView.addObject("amount", 
                paymentDetails.getString("amount") + " " + 
                paymentDetails.getString("currency"));
            modelAndView.addObject("payeeEmail", paymentDetails.getString("payeeEmail"));
            modelAndView.addObject("payerName", paymentDetails.getString("payerName"));
            
            return modelAndView;

        } catch (IOException e) {
            e.printStackTrace();
            // Handle error case
            modelAndView.setViewName("transfer-error");
            modelAndView.addObject("error", "Failed to execute payment: " + e.getMessage());
            return modelAndView;
        }
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(
            Principal principal, 
            @RequestParam double amount,
            @RequestParam String recipientEmail) {
        
        String email = principal.getName();
        Member sender = memberRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        long userID = sender.getId();

        try {
            JSONObject paymentResponse = payPalService.createPayment(
                amount, 
                "USD", 
                "Payment transaction", 
                userID,
                recipientEmail
            );
            
            return ResponseEntity.ok(paymentResponse.toMap());
            
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to initiate payment: " + e.getMessage()));
        }
    }

    @GetMapping("/cancel")
    public ModelAndView paymentCancel() {
        ModelAndView modelAndView = new ModelAndView("transfer-cancel");
        modelAndView.addObject("message", "Payment was cancelled");
        return modelAndView;
    }
}