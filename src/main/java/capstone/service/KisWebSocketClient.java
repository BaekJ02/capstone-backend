package capstone.service;

import capstone.dto.StockPriceDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
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

    private void sendSubscribe(String symbol, String approvalKey, boolean subscribe) {
        String trType = subscribe ? "1" : "2";
        String message = String.format(
            "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"}," +
            "\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"%s\"}}}",
            approvalKey, trType, symbol
        );
        wsClient.send(message);
    }

    private void handleMessage(String message) {
        try {
            if (message.startsWith("{")) return;

            String[] parts = message.split("\\|");
            if (parts.length < 4) return;

            String[] fields = parts[3].split("\\^");
            if (fields.length < 6) return;

            String symbol = fields[0];
            String price = fields[2];
            String change = fields[4];
            String changePercent = fields[5];

            StockPriceDto dto = new StockPriceDto();
            dto.setSymbol(symbol);
            dto.setPrice(price);
            dto.setChange(change);
            dto.setChangePercent(changePercent);

            messagingTemplate.convertAndSend("/topic/domestic/" + symbol, dto);

        } catch (Exception e) {
            log.error("메시지 파싱 오류: {}", e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
