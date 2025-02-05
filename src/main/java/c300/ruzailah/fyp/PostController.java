package c300.ruzailah.fyp;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @GetMapping("/socialfeed")
    public String socialFeed(Principal principal,Model model) {
        String email  = principal.getName();
        Optional<Member> member = memberRepository.findByEmail(email);
        List<Post> posts = postRepository.findPostsByFriends(member.get().getId());
        List<Post> returnedPosts = new ArrayList<>();
        for (Post post : posts)    {
            if (post.getMember().getId() != member.get().getId()) {
                returnedPosts.add(post);
            }
        }
        model.addAttribute("posts", posts);
        model.addAttribute("member", member.get());
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
        try {

            String email = principal.getName();
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new Exception("Member not found"));
            Post newPost = postService.createPost(title, content, image, member);
            redirectAttributes.addFlashAttribute("message", "Post created successfully!");
            return "redirect:/socialfeed";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating post: " + e.getMessage());
            return "redirect:/posts/create";
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
    public String reportPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.reportPost(id);
        return "redirect:/socialfeed";
    }
}
