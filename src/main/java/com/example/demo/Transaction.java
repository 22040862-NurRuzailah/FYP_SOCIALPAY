package com.example.demo;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false, unique = true)
    private Long paymentId;

    private String paypalID;

    private long userID;

    private LocalDate transactionDate;

    private double transactionAmount;

    private String transactionType;
    @Column(name = "flagged", nullable = false)
    private Boolean flagged = false; 

    @Column(name = "flagged_automatically", nullable = false)
    private Boolean flaggedAutomatically = false;

    @Column(name = "flag_reason", nullable = true)
    private String flagReason;

    @Column(name = "ip_address", nullable = true)
    private String ipAddress;

    private Long recieverID;

    @Column(name = "reported", nullable = false)
    private Boolean reported = false;

    @Column(name = "cleared", nullable = true)
    private Boolean cleared = false;

    @Column(name = "recieverName", nullable = true)
    private String rcvName;
    

    public String getRcvName() {
        return this.rcvName;
    }

    public void setRcvName(String rcvName) {
        this.rcvName = rcvName;
    }
    public Boolean isCleared() {
        return this.cleared;
    }

    public Boolean getCleared() {
        return this.cleared;
    }

    public void setCleared(Boolean cleared) {
        this.cleared = cleared;
    }


    public Transaction paymentId(Long paymentId) {
        setPaymentId(paymentId);
        return this;
    }

    public Transaction paypalID(String paypalID) {
        setPaypalID(paypalID);
        return this;
    }

    public Transaction userID(long userID) {
        setUserID(userID);
        return this;
    }

    public Transaction transactionDate(LocalDate transactionDate) {
        setTransactionDate(transactionDate);
        return this;
    }

    public Transaction transactionAmount(double transactionAmount) {
        setTransactionAmount(transactionAmount);
        return this;
    }

    public Transaction transactionType(String transactionType) {
        setTransactionType(transactionType);
        return this;
    }

    public Transaction flagged(Boolean flagged) {
        setFlagged(flagged);
        return this;
    }

    public Transaction flaggedAutomatically(Boolean flaggedAutomatically) {
        setFlaggedAutomatically(flaggedAutomatically);
        return this;
    }

    public Transaction flagReason(String flagReason) {
        setFlagReason(flagReason);
        return this;
    }

    public Transaction ipAddress(String ipAddress) {
        setIpAddress(ipAddress);
        return this;
    }

    public Transaction recieverID(Long recieverID) {
        setRecieverID(recieverID);
        return this;
    }

    public Transaction reported(Boolean reported) {
        setReported(reported);
        return this;
    }

  

    public Boolean isReported() {
        return this.reported;
    }

    public Boolean getReported() {
        return this.reported;
    }

    public void setReported(Boolean reported) {
        this.reported = reported;
    }

    public Long getRecieverID() {
        return this.recieverID;
    }

    public void setRecieverID(Long recieverID) {
        this.recieverID = recieverID;
    }

    public Long getPaymentId() {
        return this.paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public long getUserID() {
        return this.userID;
    }

    public String getPaypalID() {
        return this.paypalID;
    }

    public void setPaypalID(String paypalID) {
        this.paypalID = paypalID;
    }




    public void setUserID(long userID) {
        this.userID = userID;
    }

    public LocalDate getTransactionDate() {
        return this.transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public double getTransactionAmount() {
        return this.transactionAmount;
    }

    public void setTransactionAmount(double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionType() {
        return this.transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Boolean isFlagged() {
        return this.flagged;
    }

    public Boolean getFlagged() {
        return this.flagged;
    }

    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
    }

    public Boolean isFlaggedAutomatically() {
        return this.flaggedAutomatically;
    }

    public Boolean getFlaggedAutomatically() {
        return this.flaggedAutomatically;
    }

    public void setFlaggedAutomatically(Boolean flaggedAutomatically) {
        this.flaggedAutomatically = flaggedAutomatically;
    }

    public String getFlagReason() {
        return this.flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Transaction that = (Transaction) o;
        return Objects.equals(paymentId, that.paymentId) &&
                Objects.equals(transactionDate, that.transactionDate) &&
                Objects.equals(transactionAmount, that.transactionAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, transactionDate, transactionAmount);
    }

}
