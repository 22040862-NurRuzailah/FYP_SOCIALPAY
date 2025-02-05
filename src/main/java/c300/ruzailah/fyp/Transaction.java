package c300.ruzailah.fyp;

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
