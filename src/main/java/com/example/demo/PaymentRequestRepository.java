package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends  JpaRepository<PaymentRequest, Long> {
    List<PaymentRequest> findByReqRecieverID(long reqRecieverID); // Find requests for a specific receiver
    List<PaymentRequest> findByUserID(long userID); // Find requests sent by a specific user
}
