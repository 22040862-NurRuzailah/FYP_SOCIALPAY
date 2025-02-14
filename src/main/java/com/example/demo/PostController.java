package com.example.demo;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ReportedPostsRepository reportedPostsRepository;

    @Controller
    public class SocialFeedController {

        @GetMapping("/socialfeed")
        public String socialFeed(Principal principal, Model model,
                                 @RequestParam(required = false, defaultValue = "false") Boolean userPosts) {
            String email = principal.getName();
            Optional<Member> member = memberRepository.findByEmail(email);
        
            if (member.isEmpty()) {
                return "error"; // Handle case where user is not found
            }
        
            List<Post> posts;
        
            if (userPosts) {
                posts = postRepository.findByMemberIdOrderByTimestampDesc(member.get().getId());
            } else {
                posts = postRepository.findPostsByFriends(member.get().getId());
                posts = posts.stream()
                             .filter(post -> !post.isDisabled())
                             .collect(Collectors.toList());
            }
        
            model.addAttribute("posts", posts);
            model.addAttribute("member", member.get());
            model.addAttribute("showingUserPosts", userPosts);
        
            return "socialfeed";
        }
        
        @GetMapping("/post/image/{id}")
        @ResponseBody
        public ResponseEntity<byte[]> getPostImage(@PathVariable Long id) {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(post.getImage());
        }

        @PostMapping("/posts/create")
        public String createPost(@RequestParam("title") String title,
                @RequestParam("content") String content,
                @RequestParam("image") MultipartFile image,
                Principal principal,
                RedirectAttributes redirectAttributes) {
            String email = principal.getName();
            Member member = memberRepository.findByEmail(email).get();
            try {
                postService.createPost(title, content, image, member);
                redirectAttributes.addFlashAttribute("message", "Post created successfully!");
                return "redirect:/socialfeed";
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Error creating post: " + e.getMessage());
                System.out.println("Error creating post: " + e.getMessage());
                return "redirect:/socialfeed";
            }

        }

        @PostMapping("/posts/{id}/like")
        public String likePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
            postService.likePosts(id);
            return "redirect:/socialfeed";
        }

        @PostMapping("/posts/{id}/comment")
        public String createComment(@PathVariable Long id, @RequestParam("content") String content,
                Principal principal, RedirectAttributes redirectAttributes) {
            try {
                String email = principal.getName();
                Member member = memberRepository.findByEmail(email)
                        .orElseThrow(() -> new Exception("Member not found"));
                Post post = postRepository.findById(id)
                        .orElseThrow(() -> new Exception("Post not found"));
                postService.createComment(post, content, member);
                redirectAttributes.addFlashAttribute("message", "Comment created successfully!");
                return "redirect:/socialfeed";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error creating comment: " + e.getMessage());
                return "redirect:/socialfeed";
            }
        }

        @PostMapping("/posts/{id}/report")
        public String reportPost(@PathVariable Long id, @RequestParam("reason") String reason,
                Principal principal) {
            Member member = memberRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Member not found"));
            postService.reportPost(id, reason, member);
            return "redirect:/socialfeed";
        }

        @PostMapping("/posts/{id}/delete")
        public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            post.setDisabled(true);
            postRepository.save(post);
            List<ReportedPosts> reportedPosts = reportedPostsRepository.findByPostId(id);
            for (ReportedPosts reportedPost : reportedPosts) {
                reportedPost.setPostDisabled(true);
                reportedPostsRepository.save(reportedPost);
            }
            return "redirect:/admin-landing";
        }

        @PostMapping("/reported-post/ignore-all/{postId}")
        public String ignoreAllReports(@PathVariable Long postId) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            List<ReportedPosts> reportedPosts = reportedPostsRepository.findByPostId(postId);
            for (ReportedPosts reportedPost : reportedPosts) {
                reportedPostsRepository.delete(reportedPost);
            }
            post.setReportedCount(0);
            post.setDisabled(false);
            post.setIsAdminReported(false);
            postRepository.save(post);
            return "redirect:/admin-landing";
        }

        @PostMapping("/posts/{id}/edit")
        public String editPost(@PathVariable Long id, @RequestParam("title") String title,
                @RequestParam("content") String content, @RequestParam("image") MultipartFile image,
                RedirectAttributes redirectAttributes) {
            try {
                postService.editPost(id, title, content, image);
                redirectAttributes.addFlashAttribute("message", "Post edited successfully!");
                return "redirect:/socialfeed?userPosts=true";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error editing post: " + e.getMessage());
                return "redirect:/socialfeed";
            }
        }

    }
   
}