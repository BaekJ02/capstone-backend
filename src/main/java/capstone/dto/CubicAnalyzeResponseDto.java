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

    @JsonProperty("cubic_score")
    private Integer cubicScore;

    @JsonProperty("description")
    private CubicDescriptionDto description;

    @Data
    public static class CellDto {
        private String x;
        private String y;
        private String z;

        @JsonProperty("cell_num")
        private Integer cellNum;
    }

    @Data
    public static class CubicDescriptionDto {
        @JsonProperty("regime_desc")
        private String regimeDesc;

        @JsonProperty("risk_desc")
        private String riskDesc;

        @JsonProperty("momentum_desc")
        private String momentumDesc;

        @JsonProperty("conclusion")
        private String conclusion;

        @JsonProperty("detail")
        private String detail;

        @JsonProperty("summary")
        private String summary;
    }
}
