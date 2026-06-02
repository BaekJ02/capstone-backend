package capstone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CubicAnalyzeResponseDto {
    private String symbol;
    private String date;
    private CellDto cell;
    private String action;

    @JsonProperty("action_code")
    private Integer actionCode;

    @JsonProperty("regime_raw")
    private Integer regimeRaw;

    @JsonProperty("risk_raw")
    private Integer riskRaw;

    @JsonProperty("momentum_raw")
    private Integer momentumRaw;

    @Data
    public static class CellDto {
        private String x;
        private String y;
        private String z;

        @JsonProperty("cell_num")
        private Integer cellNum;
    }
}
