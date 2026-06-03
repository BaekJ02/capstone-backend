package capstone.controller;

import capstone.dto.PortfolioChartDto;
import capstone.service.PortfolioChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioChartService portfolioChartService;

    @GetMapping("/chart")
    public ResponseEntity<PortfolioChartDto> getPortfolioChart(
            @RequestParam(defaultValue = "30") int days) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        PortfolioChartDto chart = portfolioChartService.getPortfolioChart(userId, days);
        return ResponseEntity.ok(chart);
    }
}
