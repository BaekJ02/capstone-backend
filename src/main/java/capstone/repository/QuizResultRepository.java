package capstone.repository;

import capstone.domain.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    Optional<QuizResult> findByUserIdAndQuizDate(Long userId, LocalDate quizDate);
    List<QuizResult> findByUserIdOrderByCreatedAtDesc(Long userId);
}
