package capstone.controller;

import capstone.dto.TradeDto;
import capstone.service.TradeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeService tradeService;

    // 매수
    @PostMapping("/buy")
    public ResponseEntity<?> buy(@RequestBody TradeDto dto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        try {
            return ResponseEntity.ok(tradeService.buy(userId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 매도
    @PostMapping("/sell")
    public ResponseEntity<?> sell(@RequestBody TradeDto dto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        try {
            return ResponseEntity.ok(tradeService.sell(userId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 보유 종목 조회
    @GetMapping("/holdings")
    public ResponseEntity<?> getHoldings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(tradeService.getHoldings(userId));
    }

    // 주문 내역 조회
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(tradeService.getOrders(userId));
    }

    // 잔고 조회
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        return ResponseEntity.ok(tradeService.getBalance(userId));
    }
}