package capstone.repository;

import capstone.domain.CubicCellLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CubicCellLogRepository extends JpaRepository<CubicCellLog, Long> {
    List<CubicCellLog> findByUserIdOrderByAnalyzedAtDesc(Long userId);
    List<CubicCellLog> findBySymbolOrderByAnalyzedAtDesc(String symbol);
    Optional<CubicCellLog> findTopBySymbolOrderByAnalyzedAtDesc(String symbol);
}
