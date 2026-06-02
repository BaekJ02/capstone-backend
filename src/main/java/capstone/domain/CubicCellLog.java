package capstone.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "cubic_cell_log")
public class CubicCellLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String symbol;
    private Integer cellX;
    private Integer cellY;
    private Integer cellZ;
    private Integer cellNum;
    private String action;
    private Integer actionCode;
    private LocalDateTime analyzedAt;

    @PrePersist
    public void prePersist() {
        analyzedAt = LocalDateTime.now();
    }
}
