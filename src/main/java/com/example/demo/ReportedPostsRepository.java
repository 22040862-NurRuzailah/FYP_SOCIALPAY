package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ReportedPostsRepository extends JpaRepository<ReportedPosts, Long>  {
    List<ReportedPosts> findByPostId(Long postId);
}
