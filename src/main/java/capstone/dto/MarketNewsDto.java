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
public class MarketNewsDto {
    private String updatedAt;
    private List<String> headlines;
    private SectorDto positive;
    private SectorDto negative;
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorDto {
        private String sector;
        private String reason;
        private List<StockDto> stocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockDto {
        private String symbol;
        private String name;
        private String changePercent;
    }
}
