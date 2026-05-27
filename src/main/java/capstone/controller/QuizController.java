package capstone.controller;

import capstone.dto.QuizDto;
import capstone.dto.QuizResultDto;
import capstone.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // 오늘의 퀴즈 조회
    @GetMapping("/today")
    public ResponseEntity<QuizDto> getTodayQuiz() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        QuizDto quiz = quizService.getTodayQuiz(userId);
        if (quiz == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(quiz);
    }

    // 정답 제출
    @PostMapping("/submit")
    public ResponseEntity<QuizResultDto> submitAnswer(@RequestBody Map<String, Object> body) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long quizId = Long.valueOf(body.get("quizId").toString());
        String answer = (String) body.get("answer");
        return ResponseEntity.ok(quizService.submitAnswer(userId, quizId, answer));
    }
}
