package com.example.demo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAll();

    List<Post> findAllByOrderByTimestampDesc();

    @Query("SELECT p FROM Post p WHERE p.member IN " +
            "(SELECT f FROM Member m JOIN m.friends f WHERE m.id = :memberId) ORDER BY p.timestamp DESC")
    List<Post> findPostsByFriends(Long memberId);

    List<Post> findByMemberId(Long memberId);
    
    List<Post> findByMemberIdOrderByTimestampDesc(Long memberId);

    @Query("SELECT p FROM Post p WHERE " +
           "(:memberId IS NULL OR p.member.id = :memberId) AND " +
           "(:startDate IS NULL OR p.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR p.timestamp <= :endDate) AND " +
           "(:year IS NULL OR YEAR(p.timestamp) = :year) AND " +
           "(:month IS NULL OR MONTH(p.timestamp) = :month)")
    List<Post> findFilteredPosts(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
