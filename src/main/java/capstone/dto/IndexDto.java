package capstone.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IndexDto {
    private String code;
    private String name;
    private String price;
    private String change;
    private String changePercent;
}
