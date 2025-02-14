package com.example.demo;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    
}
