package capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PortfolioChartDto {
    private List<DataPoint> data;
    private double currentTotal;
    private double startTotal;
    private double changeAmount;
    private double changeRate;

    @Data
    @AllArgsConstructor
    public static class DataPoint {
        private String date;
        private double value;
    }
}
