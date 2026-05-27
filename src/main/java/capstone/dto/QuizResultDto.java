package capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDto {
    private Boolean isCorrect;
    private String answer;
    private String explanation;
    private String rewardSymbol;
    private String rewardName;
    private String rewardMarket;
    private Double rewardPrice;
}
