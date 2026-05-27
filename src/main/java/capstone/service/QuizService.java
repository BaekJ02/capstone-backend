package capstone.service;

import capstone.domain.DailyQuiz;
import capstone.domain.Holding;
import capstone.domain.QuizResult;
import capstone.domain.User;
import capstone.dto.QuizDto;
import capstone.dto.QuizResultDto;
import capstone.dto.RankingItemDto;
import capstone.repository.DailyQuizRepository;
import capstone.repository.HoldingRepository;
import capstone.repository.QuizResultRepository;
import capstone.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final DailyQuizRepository dailyQuizRepository;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final MarketRankingService marketRankingService;
    private final StockService stockService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @PostConstruct
    public void init() {
        generateTodayQuizIfNeeded();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void generateTodayQuizIfNeeded() {
        LocalDate today = LocalDate.now();
        if (dailyQuizRepository.findByQuizDate(today).isPresent()) {
            log.info("오늘의 퀴즈 이미 존재: {}", today);
            return;
        }
        try {
            log.info("오늘의 퀴즈 생성 시작: {}", today);
            generateQuiz(today);
            log.info("오늘의 퀴즈 생성 완료: {}", today);
        } catch (Exception e) {
            log.error("오늘의 퀴즈 생성 실패: {}", e.getMessage());
        }
    }

    private void generateQuiz(LocalDate date) throws Exception {
        boolean isOX = new Random().nextBoolean();
        String typePrompt = isOX
            ? "O/X 퀴즈 1개를 만들어줘. 정답은 'O' 또는 'X'로만 표시해."
            : "4지선다 퀴즈 1개를 만들어줘. 보기는 4개이고 정답은 보기 텍스트 그대로 표시해.";

        String prompt = "주식 투자 관련 교육용 퀴즈를 만들어줘.\n"
            + "난이도는 초중급 수준으로, 주식 기초 개념, 지표(PER, PBR, EPS 등), 투자 전략, 시장 개념 등에서 출제해.\n"
            + typePrompt + "\n\n"
            + "반드시 아래 JSON 형식으로만 응답해. 다른 텍스트 절대 포함하지 마.\n"
            + (isOX
                ? "{\n  \"type\": \"OX\",\n  \"question\": \"퀴즈 문제\",\n  \"answer\": \"O 또는 X\",\n  \"explanation\": \"해설 한두 문장\"\n}"
                : "{\n  \"type\": \"MULTIPLE\",\n  \"question\": \"퀴즈 문제\",\n  \"options\": [\"보기1\", \"보기2\", \"보기3\", \"보기4\"],\n  \"answer\": \"정답 보기 텍스트\",\n  \"explanation\": \"해설 한두 문장\"\n}");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-haiku-4-5-20251001");
        requestBody.put("max_tokens", 500);
        requestBody.put("system", "주식 투자 교육 전문가입니다. 요청된 JSON 형식으로만 응답하세요.");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", claudeApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.exchange(
                "https://api.anthropic.com/v1/messages",
                HttpMethod.POST, request, Map.class).getBody();

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        String jsonText = (String) content.get(0).get("text");
        jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();

        Map<String, Object> parsed = objectMapper.readValue(jsonText, Map.class);

        DailyQuiz quiz = new DailyQuiz();
        quiz.setQuizDate(date);
        quiz.setQuestion((String) parsed.get("question"));
        quiz.setType((String) parsed.get("type"));
        quiz.setAnswer((String) parsed.get("answer"));
        quiz.setExplanation((String) parsed.get("explanation"));

        if ("MULTIPLE".equals(parsed.get("type"))) {
            List<String> options = (List<String>) parsed.get("options");
            quiz.setOptions(objectMapper.writeValueAsString(options));
        }

        dailyQuizRepository.save(quiz);
    }

    public QuizDto getTodayQuiz(Long userId) {
        LocalDate today = LocalDate.now();
        DailyQuiz quiz = dailyQuizRepository.findByQuizDate(today).orElse(null);
        if (quiz == null) return null;

        // 개발 모드: 항상 풀 수 있음
        // TODO: 배포 시 아래 주석 해제
        // boolean alreadySolved = quizResultRepository.findByUserIdAndQuizDate(userId, today).isPresent();
        boolean alreadySolved = false;

        List<String> options = null;
        if ("MULTIPLE".equals(quiz.getType()) && quiz.getOptions() != null) {
            try {
                options = objectMapper.readValue(quiz.getOptions(), List.class);
            } catch (Exception e) {
                log.warn("퀴즈 보기 파싱 실패: {}", e.getMessage());
            }
        }

        return QuizDto.builder()
                .id(quiz.getId())
                .question(quiz.getQuestion())
                .type(quiz.getType())
                .options(options)
                .quizDate(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .alreadySolved(alreadySolved)
                .build();
    }

    public QuizResultDto submitAnswer(Long userId, Long quizId, String answer) {
        DailyQuiz quiz = dailyQuizRepository.findById(quizId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        boolean isCorrect = quiz.getAnswer().trim().equalsIgnoreCase(answer.trim());

        QuizResultDto result = QuizResultDto.builder()
                .isCorrect(isCorrect)
                .answer(quiz.getAnswer())
                .explanation(quiz.getExplanation())
                .build();

        if (isCorrect) {
            try {
                // 국내 시가총액 TOP20 + 해외 시가총액 TOP20 풀 구성
                List<RankingItemDto> domestic = marketRankingService.getDomesticRanking("MARKET_CAP");
                List<RankingItemDto> overseas = marketRankingService.getOverseasRanking("MARKET_CAP");

                // 국내/해외 구분 추적을 위해 별도 래퍼 사용
                record StockEntry(RankingItemDto item, boolean isDomestic) {}
                List<StockEntry> pool = new ArrayList<>();
                domestic.subList(0, Math.min(20, domestic.size())).forEach(s -> pool.add(new StockEntry(s, true)));
                overseas.subList(0, Math.min(20, overseas.size())).forEach(s -> pool.add(new StockEntry(s, false)));

                if (!pool.isEmpty()) {
                    StockEntry picked = pool.get(new Random().nextInt(pool.size()));
                    RankingItemDto stock = picked.item();
                    boolean isDomestic = picked.isDomestic();

                    String market = isDomestic ? "KOSPI" : "NASDAQ";
                    double price = 0.0;
                    try {
                        if (isDomestic) {
                            price = Double.parseDouble(stockService.getDomesticStockPrice(stock.getSymbol()).getPrice());
                        } else {
                            price = Double.parseDouble(stockService.getOverseasStockPrice(stock.getSymbol(), "NAS").getPrice());
                        }
                    } catch (Exception e) {
                        price = stock.getPrice() != null ? Double.parseDouble(stock.getPrice()) : 0.0;
                    }

                    addOrUpdateHolding(user, stock.getSymbol(), stock.getName(), market, price);

                    result.setRewardSymbol(stock.getSymbol());
                    result.setRewardName(stock.getName());
                    result.setRewardMarket(market);
                    result.setRewardPrice(price);

                    QuizResult quizResult = new QuizResult();
                    quizResult.setUser(user);
                    quizResult.setQuizDate(LocalDate.now());
                    quizResult.setIsCorrect(true);
                    quizResult.setRewardSymbol(stock.getSymbol());
                    quizResult.setRewardName(stock.getName());
                    quizResult.setRewardMarket(market);
                    quizResult.setRewardPrice(price);
                    quizResultRepository.save(quizResult);
                }
            } catch (Exception e) {
                log.error("뽑기 처리 실패: {}", e.getMessage());
            }
        } else {
            QuizResult quizResult = new QuizResult();
            quizResult.setUser(user);
            quizResult.setQuizDate(LocalDate.now());
            quizResult.setIsCorrect(false);
            quizResultRepository.save(quizResult);
        }

        return result;
    }

    private void addOrUpdateHolding(User user, String symbol, String name, String market, double price) {
        Holding existing = holdingRepository.findByUserIdAndSymbol(user.getId(), symbol).orElse(null);
        if (existing != null) {
            double totalCost = existing.getAvgPrice() * existing.getQuantity() + price;
            existing.setQuantity(existing.getQuantity() + 1);
            existing.setAvgPrice(totalCost / existing.getQuantity());
            holdingRepository.save(existing);
        } else {
            Holding h = new Holding();
            h.setUser(user);
            h.setSymbol(symbol);
            h.setName(name);
            h.setMarket(market);
            h.setQuantity(1L);
            h.setAvgPrice(price);
            holdingRepository.save(h);
        }
    }
}
