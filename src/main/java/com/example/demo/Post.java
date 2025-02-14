package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Lob;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private long likes;

    private long reportedCount;

    private boolean isDisabled = false;

    private boolean isAdminReported = false;


    public boolean isIsDisabled() {
        return this.isDisabled;
    }
    public void setIsDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public boolean isIsAdminReported() {
        return this.isAdminReported;
    }

    public boolean getIsAdminReported() {
        return this.isAdminReported;
    }

    public void setIsAdminReported(boolean isAdminReported) {
        this.isAdminReported = isAdminReported;
    }


    @Lob
    @Column(length = 1000000) 
    private byte[] image;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "post")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<ReportedPosts> reportedPosts = new ArrayList<>();


    public boolean isDisabled() {
        return this.isDisabled;
    }

    public boolean getIsDisabled() {
        return this.isDisabled;
    }

    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }


    public List<ReportedPosts> getReportedPosts() {
        return this.reportedPosts;
    }

    public void setReportedPosts(List<ReportedPosts> reportedPosts) {
        this.reportedPosts = reportedPosts;
    }


    public long getReportedCount() {
        return this.reportedCount;
    }

    public void setReportedCount(long reportedCount) {
        this.reportedCount = reportedCount;
    }


    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public byte[] getImage() {
        return this.image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Member getMember() {
        return this.member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public List<Comment> getComments() {
        return this.comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
    
    public long getLikes() {
        return this.likes;
    }

    public void setLikes(long likes) {
        this.likes = likes;
    }


}