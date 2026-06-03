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

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "cell_x", nullable = false)
    private Integer cellX;

    @Column(name = "cell_y", nullable = false)
    private Integer cellY;

    @Column(name = "cell_z", nullable = false)
    private Integer cellZ;

    @Column(name = "cell_num", nullable = false)
    private Integer cellNum;

    @Column(name = "action", nullable = false, length = 4)
    private String action;

    @Column(name = "action_code", nullable = false)
    private Integer actionCode;

    @Column(name = "cubic_score")
    private Integer cubicScore;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @PrePersist
    public void prePersist() {
        analyzedAt = LocalDateTime.now();
    }
}
