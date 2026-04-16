package capstone.service;

import capstone.domain.User;
import capstone.dto.LoginDto;
import capstone.dto.SignUpDto;
import capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 회원가입
    public User signUp(SignUpDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // 나중에 암호화 추가 가능
        user.setName(dto.getName());
        user.setBalance(10000000.0); // 초기 잔고 1000만원

        return userRepository.save(user);
    }

    // 로그인
    public User login(LoginDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다."));

        if (!user.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("비밀번호가 틀렸습니다.");
        }

        return user;
    }

    // ID로 유저 조회
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));
    }
}