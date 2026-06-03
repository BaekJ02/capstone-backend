package capstone.repository;

import capstone.domain.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    List<PortfolioSnapshot> findByUserIdOrderByDateAsc(Long userId);
    List<PortfolioSnapshot> findByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate start, LocalDate end);
    Optional<PortfolioSnapshot> findByUserIdAndDate(Long userId, LocalDate date);

    @Modifying
    @Transactional
    @Query("DELETE FROM PortfolioSnapshot s WHERE s.user.id = :userId AND s.date < :cutoffDate")
    void deleteOldSnapshots(Long userId, LocalDate cutoffDate);
}
