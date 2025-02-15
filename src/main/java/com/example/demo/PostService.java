package com.example.demo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@EnableScheduling
public class PostService {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReportedPostsRepository reportedPostsRepository;

    @Autowired 
    private NotificationRepository notificationRepository;
    
    @Autowired
    private MemberRepository memberRepository;

    public Post createPost(String title, String content, MultipartFile imageFile, Member member) throws IOException {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setTimestamp(LocalDateTime.now());
        post.setMember(member);
        
        if (imageFile != null && !imageFile.isEmpty()) {
            post.setImage(imageFile.getBytes());
        }
        
        return postRepository.save(post);
    }

    public Post editPost(Long postid, String title, String content, MultipartFile imageFile) {
        Post post = postRepository.findById(postid).get();
        post.setTitle(title);
        post.setContent(content);
        try {
            post.setImage(imageFile.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return postRepository.save(post);
    }

    public void createComment(Post post, String content, Member member) {
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setTimestamp(LocalDateTime.now());
        comment.setPost(post);
        comment.setMember(member);
        commentRepository.save(comment);
    }


    public void likePosts(long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            post.setLikes(post.getLikes() + 1);
            postRepository.save(post);
        }
    }


    public void reportPost(long id, String reason, Member member) {
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            post.setReportedCount(post.getReportedCount() + 1);
            postRepository.save(post);
            ReportedPosts reportedPost = new ReportedPosts();
            reportedPost.setPost(post);
            reportedPost.setReason(reason);
            reportedPostsRepository.save(reportedPost);
            if (member.getRole().equals("ROLE_ADMIN")) {
                post.setIsAdminReported(true);
                postRepository.save(post);
            }

            Notification notification = new Notification();
            notification.setMessage("Post ID " + post.getId() + " has been reported for reason: " + "(" +reason + ")");
            notificationRepository.save(notification);
        }
    }
    
    @Scheduled(cron = "0 * * * * ?")  
    public void banUserOnDisablePosts() {
        List<Member> members = memberRepository.findAll();
    
        for (Member member : members) {
            long disabledPostCount = postRepository.countByMemberAndDisabled(member, true);
    
            if (disabledPostCount > 3) {
                member.setEnabled(false);
                memberRepository.save(member); 
            }
        }
    }
}
