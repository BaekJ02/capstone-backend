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
        subscriptionService.subscribeDomesticOnly(symbol);
    }

    // 국내 주식 구독 취소
    @MessageMapping("/unsubscribe/domestic")
    public void unsubscribeDomestic(@Payload String symbol) {
        subscriptionService.unsubscribeDomestic(symbol);
    }

    // 미국 주식 구독
    @MessageMapping("/subscribe/overseas")
    public void subscribeOverseas(@Payload String symbol) {
        subscriptionService.subscribeOverseasOnly(symbol);
    }

    // 미국 주식 구독 취소
    @MessageMapping("/unsubscribe/overseas")
    public void unsubscribeOverseas(@Payload String symbol) {
        subscriptionService.unsubscribeOverseas(symbol);
    }

    // 홈화면용 국내 주식 현재가 구독
    @MessageMapping("/subscribe/domestic/price")
    public void subscribeDomesticPrice(@Payload String symbol) {
        subscriptionService.subscribeDomesticPriceOnly(symbol);
    }

    // 홈화면용 국내 주식 현재가 구독 취소
    @MessageMapping("/unsubscribe/domestic/price")
    public void unsubscribeDomesticPrice(@Payload String symbol) {
        subscriptionService.unsubscribeDomesticPriceOnly(symbol);
    }

    // 미국주식 호가 구독
    @MessageMapping("/subscribe/overseas/orderbook")
    public void subscribeOverseasOrderbook(@Payload String symbol) {
        subscriptionService.subscribeOverseasOrderbook(symbol);
    }

    // 미국주식 호가 구독 취소
    @MessageMapping("/unsubscribe/overseas/orderbook")
    public void unsubscribeOverseasOrderbook(@Payload String symbol) {
        subscriptionService.unsubscribeOverseasOrderbook(symbol);
    }

    // 시간외 단일가 체결 구독
    @MessageMapping("/subscribe/domestic/aftermarket")
    public void subscribeAfterMarket(@Payload String symbol) {
        log.info("시간외 체결 구독 요청: {}", symbol);
        subscriptionService.subscribeAfterMarket(symbol);
    }

    // 시간외 단일가 체결 구독 취소
    @MessageMapping("/unsubscribe/domestic/aftermarket")
    public void unsubscribeAfterMarket(@Payload String symbol) {
        log.info("시간외 체결 구독 취소: {}", symbol);
        subscriptionService.unsubscribeAfterMarket(symbol);
    }

    // 시간외 단일가 호가 구독
    @MessageMapping("/subscribe/domestic/aftermarket/orderbook")
    public void subscribeAfterMarketOrderbook(@Payload String symbol) {
        log.info("시간외 호가 구독 요청: {}", symbol);
        subscriptionService.subscribeAfterMarketOrderbook(symbol);
    }

    // 시간외 단일가 호가 구독 취소
    @MessageMapping("/unsubscribe/domestic/aftermarket/orderbook")
    public void unsubscribeAfterMarketOrderbook(@Payload String symbol) {
        log.info("시간외 호가 구독 취소: {}", symbol);
        subscriptionService.unsubscribeAfterMarketOrderbook(symbol);
    }
}
