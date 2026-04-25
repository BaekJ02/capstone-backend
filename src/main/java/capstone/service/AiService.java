package capstone.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient webClient;

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    private static final String SYSTEM_PROMPT =
            "당신은 주식 투자 전문 AI 어시스턴트입니다. 사용자의 투자 관련 질문에 친절하고 전문적으로 답변해주세요. " +
            "단, 모든 분석과 추천은 어디까지나 참고용이며 최종 투자 결정은 반드시 사용자 본인이 해야 한다는 점을 " +
            "항상 명심하고 강요나 단정적 표현은 피해주세요.";

    public String chat(String userMessage, List<Map<String, String>> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (history != null) {
            messages.addAll(history);
        }
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-haiku-4-5-20251001");
        requestBody.put("max_tokens", 1024);
        requestBody.put("system", SYSTEM_PROMPT);
        requestBody.put("messages", messages);

        try {
            Map<?, ?> response = webClient.post()
                    .uri(apiUrl + "/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            return (String) content.get(0).get("text");
        } catch (Exception e) {
            return "AI 응답 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
