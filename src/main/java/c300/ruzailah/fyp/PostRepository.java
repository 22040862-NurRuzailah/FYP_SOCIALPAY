package c300.ruzailah.fyp;

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
}
