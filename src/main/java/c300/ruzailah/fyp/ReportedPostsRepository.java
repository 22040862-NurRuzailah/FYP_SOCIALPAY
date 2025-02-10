package c300.ruzailah.fyp;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ReportedPostsRepository extends JpaRepository<ReportedPosts, Long>  {
    List<ReportedPosts> findByPostId(Long postId);
}
