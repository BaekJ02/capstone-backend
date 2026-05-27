package capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDto {
    private Long id;
    private String question;
    private String type; // OX or MULTIPLE
    private List<String> options; // 4지선다 보기 (OX면 null)
    private String quizDate;
    private Boolean alreadySolved; // 오늘 이미 풀었는지
    private Boolean isCorrect; // 이미 풀었으면 정답 여부
}
