package capstone.controller;

import capstone.service.StockSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StockSubscriptionController {

    private final StockSubscriptionService subscriptionService;

    // 국내 주식 구독
    @MessageMapping("/subscribe/domestic")
    public void subscribeDomestic(@Payload String symbol) {
        log.info("국내주식 구독 요청: {}", symbol);
        subscriptionService.subscribeDomesticOnly(symbol);
    }

    // 국내 주식 구독 취소
    @MessageMapping("/unsubscribe/domestic")
    public void unsubscribeDomestic(@Payload String symbol) {
        log.info("국내주식 구독 취소: {}", symbol);
        subscriptionService.unsubscribeDomestic(symbol);
    }

    // 미국 주식 구독
    @MessageMapping("/subscribe/overseas")
    public void subscribeOverseas(@Payload String symbol) {
        log.info("미국주식 구독 요청: {}", symbol);
        subscriptionService.subscribeOverseasOnly(symbol);
    }

    // 미국 주식 구독 취소
    @MessageMapping("/unsubscribe/overseas")
    public void unsubscribeOverseas(@Payload String symbol) {
        log.info("미국주식 구독 취소: {}", symbol);
        subscriptionService.unsubscribeOverseas(symbol);
    }

    // 홈화면용 국내 주식 현재가 구독
    @MessageMapping("/subscribe/domestic/price")
    public void subscribeDomesticPrice(@Payload String symbol) {
        log.info("국내주식 현재가 구독 요청: {}", symbol);
        subscriptionService.subscribeDomesticPriceOnly(symbol);
    }

    // 홈화면용 국내 주식 현재가 구독 취소
    @MessageMapping("/unsubscribe/domestic/price")
    public void unsubscribeDomesticPrice(@Payload String symbol) {
        log.info("국내주식 현재가 구독 취소: {}", symbol);
        subscriptionService.unsubscribeDomesticPriceOnly(symbol);
    }
}
