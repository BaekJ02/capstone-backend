package capstone.repository;

import capstone.domain.CubicCellLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CubicCellLogRepository extends JpaRepository<CubicCellLog, Long> {
    List<CubicCellLog> findByUserIdOrderByAnalyzedAtDesc(Long userId);
    List<CubicCellLog> findBySymbolOrderByAnalyzedAtDesc(String symbol);
}
