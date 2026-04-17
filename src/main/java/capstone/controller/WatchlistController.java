package capstone.controller;

import capstone.dto.WatchlistDto;
import capstone.service.WatchlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    // 관심 종목 추가
    @PostMapping
    public ResponseEntity<?> add(@RequestBody WatchlistDto dto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        try {
            return ResponseEntity.ok(watchlistService.add(userId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 관심 종목 삭제
    @DeleteMapping("/{symbol}")
    public ResponseEntity<?> remove(@PathVariable String symbol, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        try {
            return ResponseEntity.ok(watchlistService.remove(userId, symbol));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 관심 종목 조회
    @GetMapping
    public ResponseEntity<?> getList(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(watchlistService.getList(userId));
    }
}