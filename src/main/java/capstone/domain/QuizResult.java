package capstone.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "quiz_result")
public class QuizResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate quizDate;

    @Column(nullable = false)
    private Boolean isCorrect;

    @Column(length = 20)
    private String rewardSymbol; // 뽑기로 받은 종목 코드

    @Column(length = 100)
    private String rewardName; // 종목명

    @Column(length = 20)
    private String rewardMarket; // KOSPI, NASDAQ 등

    @Column
    private Double rewardPrice; // 지급 당시 현재가

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
