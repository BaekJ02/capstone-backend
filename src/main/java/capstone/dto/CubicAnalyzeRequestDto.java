package capstone.dto;

import lombok.Data;
import java.util.List;

@Data
public class CubicAnalyzeRequestDto {
    private String symbol;
    private List<OhlcvRowDto> ohlcv;

    @Data
    public static class OhlcvRowDto {
        private String date;
        private double open;
        private double high;
        private double low;
        private double close;
        private double volume;
    }
}
