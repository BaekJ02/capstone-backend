package capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorTrendDto {
    private String date;
    private String closePrice;
    private String personalNet;
    private String foreignNet;
    private String institutionNet;
}
