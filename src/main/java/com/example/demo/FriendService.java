package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FriendService {

    private static final Logger logger = LoggerFactory.getLogger(FriendService.class);

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    public void sendFriendRequests(Long senderId, Long receiverId) {
        Member sender = memberRepository.findById(senderId).orElse(null);
        Member receiver = memberRepository.findById(receiverId).orElse(null);
        if (sender != null && receiver != null) {
            FriendRequest friendRequest = new FriendRequest();
            friendRequest.setSender(sender);
            friendRequest.setReceiver(receiver);
            friendRequest.setStatus(FriendRequest.FriendRequestStatus.PENDING);
            friendRequestRepository.save(friendRequest);
        } else {
            throw new IllegalArgumentException("Invalid sender or receiver ID");
        }
    }

    public void acceptFriendRequest(Long requestId) {
        Optional<FriendRequest> friendRequestOptional = friendRequestRepository.findById(requestId);
        if (friendRequestOptional.isPresent()) {
            FriendRequest friendRequest = friendRequestOptional.get();
            friendRequest.setStatus(FriendRequest.FriendRequestStatus.ACCEPTED);
            friendRequestRepository.save(friendRequest);
            Member sender = friendRequest.getSender();
            Member receiver = friendRequest.getReceiver();
            sender.getFriends().add(receiver);
            receiver.getFriends().add(sender);
            memberRepository.save(sender);
            memberRepository.save(receiver);
        } else {
            throw new IllegalArgumentException("Friend request not found");
        }
    }

    public void rejectFriendRequest(Long requestId) {
        Optional<FriendRequest> friendRequestOptional = friendRequestRepository.findById(requestId);
        if (friendRequestOptional.isPresent()) {
            FriendRequest friendRequest = friendRequestOptional.get();
            friendRequest.setStatus(FriendRequest.FriendRequestStatus.REJECTED);
            friendRequestRepository.save(friendRequest);
            memberRepository.save(friendRequest.getReceiver());
            memberRepository.save(friendRequest.getSender());
            logger.info("Friend request rejected");
        } else {
            throw new IllegalArgumentException("Friend request not found");
        }
    }

    public List<FriendRequest> getSentFriendRequests(Long memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null) {
            return member.getSentRequests();
        } else {
            throw new IllegalArgumentException("Member not found");
        }
    }

    public List<FriendRequest> getReceivedFriendRequests(Long memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null) {
            return member.getReceivedRequests();
        } else { 
            throw new IllegalArgumentException("Member not found");
        }
    }

    public void removeFriend(Long memberId, Long friendId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        Member friend = memberRepository.findById(friendId).orElse(null);
        if (member != null && friend != null) {
            member.getFriends().remove(friend);
            friend.getFriends().remove(member);
            memberRepository.save(member);
            memberRepository.save(friend);
        } else {
            throw new IllegalArgumentException("Member or friend not found");
        }
    }
}
