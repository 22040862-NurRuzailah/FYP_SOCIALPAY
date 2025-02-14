package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class FriendController {
    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    @Autowired
    private FriendService friendService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @GetMapping("/add-friends")
    public String getFriendsPage(Model model, Principal principal) {
        Member member = getCurrentMember(principal);
        List<Member> friends = member.getFriends();
        List<FriendRequest> sentRequests = checkFriendReqStatus(member.getSentRequests());
        List<FriendRequest> receivedRequests = checkFriendReqStatus(member.getReceivedRequests());
        List<PaymentRequest> receivedPaymentRequests = paymentRequestRepository.findByReqRecieverID(member.getId());
        List<PaymentRequest> sentPaymentRequests = paymentRequestRepository.findByUserID(member.getId());
        model.addAttribute("friends", friends);
        model.addAttribute("sentRequests", sentRequests);
        model.addAttribute("receivedRequests", receivedRequests);
        model.addAttribute("receivedPaymentRequests", receivedPaymentRequests);
        model.addAttribute("sentPaymentRequests", sentPaymentRequests);
        return "add-friends";
    }

    private List<FriendRequest> checkFriendReqStatus(List<FriendRequest> requests) {
        List<FriendRequest> returnRequest = new ArrayList<>();
        for (FriendRequest request : requests) {
            if (request.getStatus() == FriendRequest.FriendRequestStatus.PENDING) {
                returnRequest.add(request);
            }
        }

        return returnRequest;
    }

    @GetMapping("/friends/search")
    @ResponseBody
    public List<Map<String, String>> searchMembers(@RequestParam String query, Principal principal) {
        Member currentMember = getCurrentMember(principal);
        return memberRepository.findByNameOrPhoneNumber(query).stream()
                .filter(member -> !member.getId().equals(currentMember.getId()))
                .map(member -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("name", member.getName());
                    result.put("email", member.getEmail());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/send-friend-request")
    public RedirectView sendFriendRequest(@RequestBody Map<String, String> requestBody,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            Member currentMember = getCurrentMember(principal);
            String email = requestBody.get("email");

            if (email == null || email.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Email is required.");
                redirectAttributes.addFlashAttribute("success", false);
                return new RedirectView("/add-friends");
            }

            // Find the receiver by email
            Optional<Member> receiver = memberRepository.findByEmail(email);

            if (receiver.isPresent()) {
                Member receiverMember = receiver.get();

                // Check if already friends, or if a friend request has been sent/received
                if (currentMember.getFriends().contains(receiverMember) ||
                        currentMember.getSentRequests().stream()
                                .anyMatch(request -> request.getReceiver().equals(receiverMember) &&
                                        request.getStatus() == FriendRequest.FriendRequestStatus.PENDING)
                        ||
                        currentMember.getReceivedRequests().stream()
                                .anyMatch(request -> request.getSender().equals(receiverMember) &&
                                        request.getStatus() == FriendRequest.FriendRequestStatus.PENDING)) {

                    redirectAttributes.addFlashAttribute("message",
                            "You are already friends with this user or a friend request is pending.");
                    redirectAttributes.addFlashAttribute("success", false);
                } else {
                    // If no conflicts, send the friend request
                    friendService.sendFriendRequests(currentMember.getId(), receiverMember.getId());
                    redirectAttributes.addFlashAttribute("message", "Friend request sent successfully!");
                    redirectAttributes.addFlashAttribute("success", true);
                }
            } else {
                redirectAttributes.addFlashAttribute("message", "User not found.");
                redirectAttributes.addFlashAttribute("success", false);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "An error occurred: " + e.getMessage());
            redirectAttributes.addFlashAttribute("success", false);
        }
        return new RedirectView("/add-friends");
    }

    @PostMapping("/accept-friend-request/{requestId}")
    public RedirectView acceptFriendRequest(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        try {
            friendService.acceptFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("message", "Friend request accepted!");
            redirectAttributes.addFlashAttribute("success", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("success", false);
        }
        return new RedirectView("/add-friends"); // Redirect to add-friends.html
    }

    @PostMapping("/reject-friend-request/{requestId}")
    public RedirectView rejectFriendRequest(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        try {
            friendService.rejectFriendRequest(requestId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("success", false);
        }
        return new RedirectView("/add-friends"); // Redirect to add-friends.html
    }

    @PostMapping("/remove-friend/{friendId}")
    public RedirectView removeFriend(@PathVariable Long friendId, RedirectAttributes redirectAttributes,
            Principal principal) {

        Member currentMember = getCurrentMember(principal);
        friendService.removeFriend(currentMember.getId(), friendId);

        return new RedirectView("/add-friends");
    }

    private Member getCurrentMember(Principal principal) {
        if (principal == null) {
            logger.warn("[FriendController] Principal is null. User is not authenticated.");
            throw new RuntimeException("User is not authenticated. Please log in.");
        }

        String email = principal.getName();
        if (email == null || email.isEmpty()) {
            logger.warn("[FriendController] Principal email is null or empty.");
            throw new RuntimeException("User is not authenticated.");
        }

        logger.info("[FriendController] Resolving email for Principal: {}", email);

        String resolvedEmail = "system".equalsIgnoreCase(email) ? "system.user@example.com" : email;

        Optional<Member> memberOpt = memberRepository.findByEmail(resolvedEmail);
        if (memberOpt.isPresent()) {
            return memberOpt.get();
        } else {
            logger.error("[FriendController] No member found for email: {}", resolvedEmail);
            throw new RuntimeException("No member found for email: " + resolvedEmail);
        }

    }

    @PostMapping("/send-payment-request/{friendId}")
    public String sendPaymentRequest(@PathVariable Long friendId, @RequestParam("amount") float amount, 
    @RequestParam("reason") String reason, 
            Principal principal) {
        Member currentMember = getCurrentMember(principal);
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setUserID(currentMember.getId());
        paymentRequest.setReqRecieverID(friendId);
        paymentRequest.setAmount(amount);
        paymentRequest.setReason(reason);
        paymentRequest.setIsApproved(false);
        paymentRequest.setRecieverName(memberRepository.findById(friendId).get().getName());
        paymentRequest.setSenderName(currentMember.getName());
        paymentRequestRepository.save(paymentRequest);
        return "redirect:/add-friends";
    }

}
