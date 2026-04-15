package capstone.service;

import capstone.dto.StockSearchDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.InputStreamReader;

@Service
public class StockSearchService {

    private final List<StockSearchDto> stockList = new ArrayList<>();

    @PostConstruct
    public void loadStockData() {
        loadCsv("stocks.csv", "주식");
        loadCsv("etf.csv", "ETF");
    }

    private void loadCsv(String filename, String type) {
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(
                        Objects.requireNonNull(
                                getClass().getClassLoader().getResourceAsStream(filename)),
                        StandardCharsets.UTF_8))
                .withSkipLines(1)
                .build()) {

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 2) continue;

                StockSearchDto dto = new StockSearchDto();

                if (type.equals("ETF")) {
                    dto.setSymbol(line[0].trim());  // 종목코드
                    dto.setName(line[1].trim());    // 종목명
                    dto.setMarket("ETF");
                } else {
                    dto.setSymbol(line[1].trim());  // 단축코드
                    dto.setName(line[3].trim());    // 한글종목약명
                    dto.setMarket(line[6].trim());  // 시장구분
                }

                stockList.add(dto);
            }

            System.out.println(type + " 데이터 로드 완료: " + stockList.size() + "개");

        } catch (Exception e) {
            System.err.println(type + " 데이터 로드 실패: " + e.getMessage());
        }
    }

    public List<StockSearchDto> search(String keyword) {
        return stockList.stream()
                .filter(stock ->
                        stock.getName().contains(keyword) ||
                                stock.getSymbol().contains(keyword))
                .limit(20) // 최대 20개만 반환
                .collect(Collectors.toList());
    }
}