package capstone.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "daily_quiz")
public class DailyQuiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate quizDate;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, length = 10)
    private String type; // OX or MULTIPLE

    @Column(length = 1000)
    private String options; // JSON 배열 문자열 (4지선다일 때만 사용)

    @Column(nullable = false, length = 200)
    private String answer; // OX: "O" or "X", MULTIPLE: 정답 텍스트

    @Column(length = 500)
    private String explanation; // 해설
}
