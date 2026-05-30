package capstone.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisAuthService {

    private final RestTemplate restTemplate;

    @Value("${kis.api.key}")
    private String appKey;

    @Value("${kis.api.secret}")
    private String appSecret;

    @Value("${kis.api.url}")
    private String baseUrl;

    private String accessToken;
    private LocalDateTime tokenExpireTime;

    // 웹소켓 접속키 (approval_key)
    private String approvalKey;

    public synchronized String getApprovalKey() {
        if (approvalKey != null) return approvalKey;
        return issueApprovalKey();
    }

    private synchronized String issueApprovalKey() {
        if (approvalKey != null) return approvalKey;

        String url = baseUrl + "/oauth2/Approval";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("secretkey", appSecret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        approvalKey = (String) response.get("approval_key");
        return approvalKey;
    }

    public synchronized String getAccessToken() {
        if (accessToken != null && LocalDateTime.now().isBefore(tokenExpireTime.minusHours(1))) {
            return accessToken;
        }
        return issueAccessToken();
    }

    private synchronized String issueAccessToken() {
        // 이미 다른 쓰레드가 발급했을 수 있으니 다시 체크
        if (accessToken != null && LocalDateTime.now().isBefore(tokenExpireTime.minusHours(1))) {
            return accessToken;
        }

        String url = baseUrl + "/oauth2/tokenP";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("appsecret", appSecret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        accessToken = (String) response.get("access_token");
        tokenExpireTime = LocalDateTime.now().plusHours(24);

        return accessToken;
    }
}