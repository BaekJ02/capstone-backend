package capstone.controller;

import capstone.domain.User;
import capstone.dto.LoginDto;
import capstone.dto.SignUpDto;
import capstone.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpDto dto) {
        try {
            User user = userService.signUp(dto);
            return ResponseEntity.ok("회원가입 성공! ID: " + user.getId());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto dto, HttpSession session) {
        try {
            User user = userService.login(dto);
            session.setAttribute("userId", user.getId());
            return ResponseEntity.ok("로그인 성공! 환영합니다, " + user.getName());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃 성공");
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        User user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }
}