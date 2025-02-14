package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String name, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP for SocialPay Account Verification");
        message.setText("Dear " + name + ",\n\n" +
                "Your OTP for verifying your SocialPay account is: " + otp + "\n\n" +
                "This OTP is valid for 5 minutes.\n\n" +
                "Best Regards,\nSocialPay Team");

        mailSender.send(message);
    }

}

