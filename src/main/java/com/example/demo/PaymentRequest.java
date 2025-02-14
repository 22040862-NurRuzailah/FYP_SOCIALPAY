package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class PaymentRequest {
 
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private long userID;

    private long reqRecieverID;

    private float amount;

    private String reason;

    private boolean isApproved = false;

    private String senderName ;

    private String recieverName ;


    public String getSenderName() {
        return this.senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getRecieverName() {
        return this.recieverName;
    }

    public void setRecieverName(String recieverName) {
        this.recieverName = recieverName;
    }


    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isIsApproved() {
        return this.isApproved;
    }

    public boolean getIsApproved() {
        return this.isApproved;
    }

    public void setIsApproved(boolean isApproved) {
        this.isApproved = isApproved;
    }


    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getUserID() {
        return this.userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public long getReqRecieverID() {
        return this.reqRecieverID;
    }

    public void setReqRecieverID(long reqRecieverID) {
        this.reqRecieverID = reqRecieverID;
    }

    public float getAmount() {
        return this.amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }


}
