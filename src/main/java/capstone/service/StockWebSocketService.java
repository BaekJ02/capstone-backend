package capstone.service;

import capstone.dto.StockPriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class StockWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StockService stockService;
    private final StockSubscriptionService subscriptionService;
    private final KisWebSocketClient kisWebSocketClient;
    private final MarketTimeService marketTimeService;

    // 국내 주식 - 시간대별 처리
    @Scheduled(fixedDelay = 3000)
    public void sendDomesticStockPrices() {
        for (String symbol : subscriptionService.getDomesticSymbols()) {
            try {
                if (marketTimeService.isKoreanMarketOpen()) {
                    // 정규장: KIS 웹소켓이 알아서 보내줌
                    kisWebSocketClient.subscribe(symbol);
                } else if (marketTimeService.isKoreanExtendedHours()) {
                    // 시간외: 3초마다 시간외 단일가 REST API 조회
                    StockPriceDto price = stockService.getDomesticOverTimePrice(symbol);
                    messagingTemplate.convertAndSend("/topic/domestic/" + symbol, price);
                } else {
                    // 야간/주말: 종가 1회 조회
                    StockPriceDto price = stockService.getDomesticStockPrice(symbol);
                    messagingTemplate.convertAndSend("/topic/domestic/" + symbol, price);
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("국내주식 처리 실패: {}", e.getMessage());
            }
        }
    }

    // 미국 주식 - 시간대별 처리 (일단 REST API 유지, 추후 웹소켓 추가)
    @Scheduled(fixedDelay = 3000, initialDelay = 2000)
    public void sendOverseasStockPrices() {
        for (String symbolWithExchange : subscriptionService.getOverseasSymbols()) {
            try {
                String[] parts = symbolWithExchange.split(",");
                String symbol = parts[0];
                String exchange = parts.length > 1 ? parts[1] : "NAS";
                StockPriceDto price = stockService.getOverseasStockPrice(symbol, exchange);
                messagingTemplate.convertAndSend("/topic/overseas/" + symbol, price);
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("해외주식 조회 실패: {}", e.getMessage());
            }
        }
    }
}
