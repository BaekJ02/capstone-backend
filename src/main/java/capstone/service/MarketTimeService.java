package capstone.service;

import org.springframework.stereotype.Service;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class MarketTimeService {

    // 한국 정규장 여부
    public boolean isKoreanMarketOpen() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
            date.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        return now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(15, 30));
    }

    // 한국 시간외 단일가 여부 (장 전/후)
    public boolean isKoreanExtendedHours() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
            date.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        boolean preMaket = now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(9, 0));
        boolean postMarket = now.isAfter(LocalTime.of(15, 40)) && now.isBefore(LocalTime.of(18, 0));
        return preMaket || postMarket;
    }

    // 미국 정규장 여부 (썸머타임 자동 적용)
    public boolean isUsMarketOpen() {
        LocalDate date = LocalDate.now(ZoneId.of("America/New_York"));
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
            date.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        LocalTime now = LocalTime.now(ZoneId.of("America/New_York"));
        return now.isAfter(LocalTime.of(9, 30)) && now.isBefore(LocalTime.of(16, 0));
    }
}
