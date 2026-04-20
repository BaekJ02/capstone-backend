package capstone.service;

import capstone.dto.OrderBookDto;
import capstone.dto.StockPriceDto;
import capstone.dto.TradeTickDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisWebSocketClient {

    private final KisAuthService kisAuthService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${kis.ws.url:ws://ops.koreainvestment.com:21000}")
    private String wsUrl;

    private WebSocketClient wsClient;
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final Set<String> subscribedOverseasSymbols = ConcurrentHashMap.newKeySet();
    private boolean connected = false;

    @PostConstruct
    public void init() {
        connect();
    }

    public void connect() {
        try {
            String approvalKey = kisAuthService.getApprovalKey();
            wsClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected = true;
                    log.info("KIS 웹소켓 연결 성공");
                    for (String symbol : subscribedSymbols) {
                        sendSubscribe(symbol, approvalKey, true);
                    }
                    for (String symbolWithExchange : subscribedOverseasSymbols) {
                        sendOverseasSubscribe(symbolWithExchange, approvalKey, true);
                    }
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    log.warn("KIS 웹소켓 연결 끊김: {}", reason);
                    new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            connect();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("KIS 웹소켓 오류: {}", ex.getMessage());
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            log.error("KIS 웹소켓 연결 실패: {}", e.getMessage());
        }
    }

    public void subscribe(String symbol) {
        subscribedSymbols.add(symbol);
        if (connected) {
            sendSubscribe(symbol, kisAuthService.getApprovalKey(), true);
        }
    }

    public void unsubscribe(String symbol) {
        subscribedSymbols.remove(symbol);
        if (connected) {
            sendSubscribe(symbol, kisAuthService.getApprovalKey(), false);
        }
    }

    public void subscribeOverseas(String symbolWithExchange) {
        subscribedOverseasSymbols.add(symbolWithExchange);
        if (connected) {
            sendOverseasSubscribe(symbolWithExchange, kisAuthService.getApprovalKey(), true);
        }
    }

    public void unsubscribeOverseas(String symbolWithExchange) {
        subscribedOverseasSymbols.remove(symbolWithExchange);
        if (connected) {
            sendOverseasSubscribe(symbolWithExchange, kisAuthService.getApprovalKey(), false);
        }
    }

    private void sendSubscribe(String symbol, String approvalKey, boolean subscribe) {
        String trType = subscribe ? "1" : "2";
        String priceMsg = String.format(
            "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"}," +
            "\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"%s\"}}}",
            approvalKey, trType, symbol
        );
        wsClient.send(priceMsg);
        String orderbookMsg = String.format(
            "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"}," +
            "\"body\":{\"input\":{\"tr_id\":\"H0STASP0\",\"tr_key\":\"%s\"}}}",
            approvalKey, trType, symbol
        );
        wsClient.send(orderbookMsg);
    }

    private void sendOverseasSubscribe(String symbolWithExchange, String approvalKey, boolean subscribe) {
        String trType = subscribe ? "1" : "2";
        String[] parts = symbolWithExchange.split(",");
        String symbol = parts[0];
        String exchange = parts.length > 1 ? parts[1] : "NAS";
        String trKey = "D" + exchange + symbol;
        String message = String.format(
            "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"}," +
            "\"body\":{\"input\":{\"tr_id\":\"HDFSCNT0\",\"tr_key\":\"%s\"}}}",
            approvalKey, trType, trKey
        );
        wsClient.send(message);
    }

    private void handleMessage(String message) {
        try {
            if (message.startsWith("{")) return;

            String[] parts = message.split("\\|");
            if (parts.length < 4) return;

            String trId = parts[1];
            String[] fields = parts[3].split("\\^");

            if ("H0STASP0".equals(trId)) {
                handleOrderBook(fields);
            } else if ("H0STCNT0".equals(trId)) {
                if (fields.length < 6) return;
                handlePrice(fields);
                handleTradeTick(fields);
            } else if ("HDFSCNT0".equals(trId)) {
                if (fields.length < 6) return;
                String symbol = fields[0];
                String realSymbol = symbol.length() > 4 ? symbol.substring(4) : symbol;
                StockPriceDto dto = new StockPriceDto();
                dto.setSymbol(realSymbol);
                dto.setPrice(fields[2]);
                dto.setChange(fields[4]);
                dto.setChangePercent(fields[5]);
                messagingTemplate.convertAndSend("/topic/overseas/" + realSymbol, dto);
            }

        } catch (Exception e) {
            log.error("메시지 파싱 오류: {}", e.getMessage());
        }
    }

    private void handlePrice(String[] fields) {
        String symbol = fields[0];
        StockPriceDto dto = new StockPriceDto();
        dto.setSymbol(symbol);
        dto.setPrice(fields[2]);
        dto.setChange(fields[4]);
        dto.setChangePercent(fields[5]);
        messagingTemplate.convertAndSend("/topic/domestic/" + symbol, dto);
    }

    private void handleTradeTick(String[] fields) {
        if (fields.length < 14) return;
        String symbol = fields[0];
        TradeTickDto dto = new TradeTickDto();
        dto.setSymbol(symbol);
        dto.setTime(fields[1]);
        dto.setPrice(fields[2]);
        dto.setQuantity(fields[3]);
        dto.setSide("1".equals(fields[13]) ? "BUY" : "SELL");
        messagingTemplate.convertAndSend("/topic/tradetick/" + symbol, dto);
    }

    private void handleOrderBook(String[] fields) {
        if (fields.length < 45) return;
        String symbol = fields[0];

        OrderBookDto dto = new OrderBookDto();
        dto.setSymbol(symbol);

        // 매도호가: 높은가격→낮은가격 순 (i=4→0 역순), price=fields[3+i], qty=fields[23+i]
        List<OrderBookDto.OrderBookEntry> asks = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            OrderBookDto.OrderBookEntry entry = new OrderBookDto.OrderBookEntry();
            entry.setPrice(fields[3 + i]);
            entry.setQuantity(fields[23 + i]);
            asks.add(entry);
        }
        dto.setAsks(asks);

        // 매수호가: 높은가격→낮은가격 순 (i=0→4), price=fields[13+i], qty=fields[33+i]
        List<OrderBookDto.OrderBookEntry> bids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OrderBookDto.OrderBookEntry entry = new OrderBookDto.OrderBookEntry();
            entry.setPrice(fields[13 + i]);
            entry.setQuantity(fields[33 + i]);
            bids.add(entry);
        }
        dto.setBids(bids);

        dto.setTotalAskQty(fields[43]);
        dto.setTotalBidQty(fields[44]);

        messagingTemplate.convertAndSend("/topic/orderbook/" + symbol, dto);
    }

    public boolean isConnected() {
        return connected;
    }
}
