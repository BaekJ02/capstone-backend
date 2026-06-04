package capstone.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSubscriptionService {

    private final KisWebSocketClient kisWebSocketClient;

    // 구독 중인 국내 종목 목록
    private final Set<String> domesticSymbols = ConcurrentHashMap.newKeySet();

    // 구독 중인 해외 종목 목록
    private final Set<String> overseasSymbols = ConcurrentHashMap.newKeySet();

    public void subscribeDomestic(String symbol) {
        domesticSymbols.add(symbol);
    }

    public void unsubscribeDomestic(String symbol) {
        domesticSymbols.remove(symbol);
    }

    public void subscribeOverseas(String symbol) {
        overseasSymbols.add(symbol);
    }

    public void unsubscribeOverseas(String symbol) {
        overseasSymbols.remove(symbol);
        kisWebSocketClient.unsubscribeOverseas(symbol);
    }

    public Set<String> getDomesticSymbols() {
        return domesticSymbols;
    }

    public Set<String> getOverseasSymbols() {
        return overseasSymbols;
    }

    public void subscribeOverseasOnly(String symbolWithExchange) {
        if (overseasSymbols.add(symbolWithExchange)) {
            kisWebSocketClient.subscribeOverseas(symbolWithExchange);
        }
    }

    public void subscribeDomesticOnly(String symbol) {
        if (domesticSymbols.add(symbol)) {
            kisWebSocketClient.subscribe(symbol);
        }
    }

    public void subscribeDomesticPriceOnly(String symbol) {
        if (domesticSymbols.add(symbol)) {
            kisWebSocketClient.subscribePriceOnly(symbol);
        }
    }

    public void unsubscribeDomesticPriceOnly(String symbol) {
        domesticSymbols.remove(symbol);
        kisWebSocketClient.unsubscribePriceOnly(symbol);
    }

    private final Set<String> overseasOrderbookSymbols = ConcurrentHashMap.newKeySet();

    public void subscribeOverseasOrderbook(String symbolWithExchange) {
        if (overseasOrderbookSymbols.add(symbolWithExchange)) {
            kisWebSocketClient.subscribeOverseasOrderbook(symbolWithExchange);
        }
    }

    public void unsubscribeOverseasOrderbook(String symbolWithExchange) {
        overseasOrderbookSymbols.remove(symbolWithExchange);
        kisWebSocketClient.unsubscribeOverseasOrderbook(symbolWithExchange);
    }

    private final Set<String> afterMarketSymbols = ConcurrentHashMap.newKeySet();
    private final Set<String> afterMarketOrderbookSymbols = ConcurrentHashMap.newKeySet();

    public void subscribeAfterMarket(String symbol) {
        if (afterMarketSymbols.add(symbol)) {
            kisWebSocketClient.subscribeAfterMarket(symbol);
        }
    }

    public void unsubscribeAfterMarket(String symbol) {
        afterMarketSymbols.remove(symbol);
        kisWebSocketClient.unsubscribeAfterMarket(symbol);
    }

    public void subscribeAfterMarketOrderbook(String symbol) {
        if (afterMarketOrderbookSymbols.add(symbol)) {
            kisWebSocketClient.subscribeAfterMarketOrderbook(symbol);
        }
    }

    public void unsubscribeAfterMarketOrderbook(String symbol) {
        afterMarketOrderbookSymbols.remove(symbol);
        kisWebSocketClient.unsubscribeAfterMarketOrderbook(symbol);
    }

    public Set<String> getAfterMarketSymbols() {
        return afterMarketSymbols;
    }

    public void resetAll() {
        for (String symbol : new HashSet<>(domesticSymbols)) {
            kisWebSocketClient.unsubscribePriceOnly(symbol);
            kisWebSocketClient.unsubscribe(symbol);
        }
        domesticSymbols.clear();

        for (String sym : new HashSet<>(overseasSymbols)) {
            kisWebSocketClient.unsubscribeOverseas(sym);
        }
        overseasSymbols.clear();

        for (String sym : new HashSet<>(overseasOrderbookSymbols)) {
            kisWebSocketClient.unsubscribeOverseasOrderbook(sym);
        }
        overseasOrderbookSymbols.clear();

        for (String sym : new HashSet<>(afterMarketSymbols)) {
            kisWebSocketClient.unsubscribeAfterMarket(sym);
        }
        afterMarketSymbols.clear();

        for (String sym : new HashSet<>(afterMarketOrderbookSymbols)) {
            kisWebSocketClient.unsubscribeAfterMarketOrderbook(sym);
        }
        afterMarketOrderbookSymbols.clear();

        log.info("모든 WebSocket 구독 초기화 완료");
    }
}