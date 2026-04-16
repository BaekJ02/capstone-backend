package capstone.service;

import capstone.dto.StockPriceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class StockWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StockService stockService;
    private final StockSubscriptionService subscriptionService;

    // 국내 주식 - 3초마다 구독 중인 종목 전송
    @Scheduled(fixedDelay = 3000)
    public void sendDomesticStockPrices() {
        for (String symbol : subscriptionService.getDomesticSymbols()) {
            try {
                StockPriceDto price = stockService.getDomesticStockPrice(symbol);
                messagingTemplate.convertAndSend("/topic/domestic/" + symbol, price);
                Thread.sleep(100); // 0.1초 딜레이 추가
            } catch (Exception e) {
                System.err.println("국내주식 조회 실패: " + e.getMessage());
            }
        }
    }

    // 미국 주식 - 3초마다 구독 중인 종목 전송
    @Scheduled(fixedDelay = 3000, initialDelay = 2000)
    public void sendOverseasStockPrices() {
        for (String symbolWithExchange : subscriptionService.getOverseasSymbols()) {
            try {
                String[] parts = symbolWithExchange.split(",");
                String symbol = parts[0];
                String exchange = parts.length > 1 ? parts[1] : "NAS";
                StockPriceDto price = stockService.getOverseasStockPrice(symbol, exchange);
                messagingTemplate.convertAndSend("/topic/overseas/" + symbol, price);
                Thread.sleep(100); // 0.1초 딜레이 추가
            } catch (Exception e) {
                System.err.println("해외주식 조회 실패: " + e.getMessage());
            }
        }
    }
}