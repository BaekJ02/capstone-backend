package capstone.controller;

import capstone.service.StockSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionRestController {

    private final StockSubscriptionService subscriptionService;

    @PostMapping("/reset")
    public ResponseEntity<?> reset() {
        subscriptionService.resetAll();
        return ResponseEntity.ok().build();
    }
}
