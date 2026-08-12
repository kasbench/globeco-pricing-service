package org.kasbench.globeco_pricing_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that the V2 SQL migration correctly loads all 471 rows
 * from selected_prices.csv into the price table with matching values.
 *
 * <p><b>Validates: Requirements 1.3</b></p>
 * <p>Property 1: Migration data fidelity — For any row in the selected_prices.csv dataset,
 * after the V2 migration executes, the price table SHALL contain a row with matching
 * price_date ('2017-05-18'), ticker, price, and price_std values.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationDataFidelityTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationShouldInsertExactly471Rows() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM price WHERE price_date = '2017-05-18'",
                Integer.class);
        assertEquals(471, count, "V2 migration should insert exactly 471 rows with price_date = 2017-05-18");
    }

    @Test
    void migrationDataShouldMatchCsvSource() throws Exception {
        // Read all rows from the CSV source file
        List<CsvRow> csvRows = readCsvRows();
        assertEquals(471, csvRows.size(), "selected_prices.csv should contain 471 rows");

        // Verify every row in the CSV has a matching row in the database
        for (CsvRow csvRow : csvRows) {
            List<Map<String, Object>> dbRows = jdbcTemplate.queryForList(
                    "SELECT ticker, price, price_std FROM price WHERE price_date = '2017-05-18' AND ticker = ?",
                    csvRow.ticker());

            assertFalse(dbRows.isEmpty(),
                    "Database should contain a row for ticker: " + csvRow.ticker());

            Map<String, Object> dbRow = dbRows.get(0);

            // Verify price matches (compare as BigDecimal to handle scale differences)
            BigDecimal dbPrice = (BigDecimal) dbRow.get("price");
            assertEquals(0, csvRow.price().compareTo(dbPrice),
                    "Price mismatch for ticker " + csvRow.ticker()
                            + ": expected " + csvRow.price() + " but got " + dbPrice);

            // Verify price_std matches (float comparison with tolerance)
            double dbPriceStd = ((Number) dbRow.get("price_std")).doubleValue();
            assertEquals(csvRow.priceStd(), dbPriceStd, 0.0001,
                    "price_std mismatch for ticker " + csvRow.ticker()
                            + ": expected " + csvRow.priceStd() + " but got " + dbPriceStd);
        }
    }

    private List<CsvRow> readCsvRows() throws Exception {
        List<CsvRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("static/selected_prices.csv"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                // Format: date,ticker,price,price_std
                String ticker = parts[1];
                BigDecimal price = new BigDecimal(parts[2]);
                double priceStd = Double.parseDouble(parts[3]);
                rows.add(new CsvRow(ticker, price, priceStd));
            }
        }
        return rows;
    }

    private record CsvRow(String ticker, BigDecimal price, double priceStd) {}
}
