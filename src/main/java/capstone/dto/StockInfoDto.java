package capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoDto {
    private String symbol;
    private String name;
    private String market;
    private String marketCap;
    private String marketCapRank;
    private String per;
    private String pbr;
    private String aiAnalysis;
}
