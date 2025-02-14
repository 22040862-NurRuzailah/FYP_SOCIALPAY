package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private FriendRequestStatus status;
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Member sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Member receiver;

    public enum FriendRequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    public FriendRequestStatus getStatus() {
        return this.status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

    public Member getSender() {
        return this.sender;
    }

    public void setSender(Member sender) {
        this.sender = sender;
    }

    public Member getReceiver() {
        return this.receiver;
    }

    public void setReceiver(Member receiver) {
        this.receiver = receiver;
    }
    
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
