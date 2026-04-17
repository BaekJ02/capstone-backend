package capstone.service;

import capstone.domain.Holding;
import capstone.domain.Order;
import capstone.domain.User;
import capstone.dto.TradeDto;
import capstone.repository.HoldingRepository;
import capstone.repository.OrderRepository;
import capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final OrderRepository orderRepository;

    // 매수
    @Transactional
    public String buy(Long userId, TradeDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));

        double totalPrice = dto.getPrice() * dto.getQuantity();

        // 잔고 확인
        if (user.getBalance() < totalPrice) {
            throw new RuntimeException("잔고가 부족합니다.");
        }

        // 잔고 차감
        user.setBalance(user.getBalance() - totalPrice);
        userRepository.save(user);

        // 보유종목 업데이트
        Optional<Holding> existingHolding = holdingRepository.findByUserAndSymbol(user, dto.getSymbol());
        if (existingHolding.isPresent()) {
            // 기존 보유 종목이면 평균단가 재계산
            Holding holding = existingHolding.get();
            Long newQuantity = holding.getQuantity() + dto.getQuantity();
            double newAvgPrice = ((holding.getAvgPrice() * holding.getQuantity()) + totalPrice) / newQuantity;
            holding.setQuantity(newQuantity);
            holding.setAvgPrice(newAvgPrice);
            holdingRepository.save(holding);
        } else {
            // 새로운 종목이면 새로 추가
            Holding holding = new Holding();
            holding.setUser(user);
            holding.setSymbol(dto.getSymbol());
            holding.setName(dto.getName());
            holding.setMarket(dto.getMarket());
            holding.setQuantity(dto.getQuantity());
            holding.setAvgPrice(dto.getPrice());
            holdingRepository.save(holding);
        }

        // 주문 내역 저장
        Order order = new Order();
        order.setUser(user);
        order.setSymbol(dto.getSymbol());
        order.setName(dto.getName());
        order.setType("BUY");
        order.setQuantity(dto.getQuantity());
        order.setPrice(dto.getPrice());
        orderRepository.save(order);

        return "매수 완료";
    }

    // 매도
    @Transactional
    public String sell(Long userId, TradeDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));

        Holding holding = holdingRepository.findByUserAndSymbol(user, dto.getSymbol())
                .orElseThrow(() -> new RuntimeException("보유하지 않은 종목입니다."));

        // 보유수량 확인
        if (holding.getQuantity() < dto.getQuantity()) {
            throw new RuntimeException("보유 수량이 부족합니다.");
        }

        double totalPrice = dto.getPrice() * dto.getQuantity();

        // 잔고 증가
        user.setBalance(user.getBalance() + totalPrice);
        userRepository.save(user);

        // 보유종목 업데이트
        if (holding.getQuantity().equals(dto.getQuantity())) {
            // 전량 매도면 삭제
            holdingRepository.delete(holding);
        } else {
            // 일부 매도면 수량 감소
            holding.setQuantity(holding.getQuantity() - dto.getQuantity());
            holdingRepository.save(holding);
        }

        // 주문 내역 저장
        Order order = new Order();
        order.setUser(user);
        order.setSymbol(dto.getSymbol());
        order.setName(dto.getName());
        order.setType("SELL");
        order.setQuantity(dto.getQuantity());
        order.setPrice(dto.getPrice());
        orderRepository.save(order);

        return "매도 완료";
    }

    // 보유 종목 조회
    public List<Holding> getHoldings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));
        return holdingRepository.findByUser(user);
    }

    // 주문 내역 조회
    public List<Order> getOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // 잔고 조회
    public Double getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));
        return user.getBalance();
    }
}