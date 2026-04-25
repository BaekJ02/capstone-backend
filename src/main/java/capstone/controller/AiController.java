package capstone.controller;

import capstone.dto.AiChatRequestDto;
import capstone.dto.AiChatResponseDto;
import capstone.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(@RequestBody AiChatRequestDto request) {
        String reply = aiService.chat(request.getMessage(), request.getHistory());
        return ResponseEntity.ok(new AiChatResponseDto(reply));
    }
}
