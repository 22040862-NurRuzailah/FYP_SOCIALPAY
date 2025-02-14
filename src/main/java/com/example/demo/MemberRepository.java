package com.example.demo;

import java.lang.StackWalker.Option;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // Find a member by their email
    Optional<Member> findByEmail(String email);

    // Find a member by their name
    Optional<Member> findByName(String name);

    // Custom query to search member names by a query string
    @Query("SELECT m.name FROM Member m WHERE LOWER(m.name) LIKE LOWER(CONCAT(:query, '%'))")
    List<String> findNamesByQuery(@Param("query") String query);

    @Query("SELECT m FROM Member m WHERE m.name LIKE %:query% OR m.phoneNumber LIKE %:query%")
    List<Member> findByNameOrPhoneNumber(@Param("query") String query);

    List<Member> findByRole(String role);

    

    Member findByPaypalAccount(String paypalAccount);


}
