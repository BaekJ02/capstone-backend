package capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingItemDto {
    private String symbol;
    private String name;
    private String price;
    private String change;
    private String changePercent;
    private String volume;
}
