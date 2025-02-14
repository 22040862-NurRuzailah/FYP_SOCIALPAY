package com.example.demo;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transfer")
public class TransferController {

    @Autowired
    private PayPalService payPalService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

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
                    "SGD",
                    "Payment transaction",
                    userID,
                    recipientEmail);

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

    // New endpoint to fulfill a payment request
    @PostMapping("/fulfill-request")
    public ResponseEntity<?> fulfillPaymentRequest(
            Principal principal,
            @RequestParam long requestId) {
        try {
            String email = principal.getName();
            Member receiver = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            long receiverID = receiver.getId();

            PaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Payment request not found"));

            if (paymentRequest.getReqRecieverID() != receiverID) {
                throw new RuntimeException("You are not authorized to fulfill this payment request.");
            }

            // Retrieve the sendr's email
            Member sender = memberRepository.findById(paymentRequest.getUserID())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            String senderEmail = sender.getPaypalAccount();

            // Use the existing PayPal transfer functionality to fulfill the request
            ResponseEntity<?> response = initiatePayment(principal, paymentRequest.getAmount(), senderEmail);

            // Delete the payment request after fulfillment
            paymentRequestRepository.delete(paymentRequest);

            return response;
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/decline/{id}")
    public String declinePaymentRequest(@PathVariable Long id) {
        PaymentRequest paymentRequest = paymentRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment request not found"));
        paymentRequestRepository.delete(paymentRequest);
        return "redirect:/add-friends";
    }

}