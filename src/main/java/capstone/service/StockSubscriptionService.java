package capstone.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
}