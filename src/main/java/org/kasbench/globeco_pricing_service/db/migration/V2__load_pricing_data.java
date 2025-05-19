package org.kasbench.globeco_pricing_service.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class V2__load_pricing_data extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        // Step 1: Pick a random date from dates.csv
        List<String> dates = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("static/dates.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                dates.add(line.trim());
            }
        }
        if (dates.isEmpty()) throw new IllegalStateException("No dates found in dates.csv");
        String selectedDate = dates.get(new Random().nextInt(dates.size()));

        // Step 2: Read prices.csv.gz and insert rows for the selected date
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("static/prices.csv.gz");
             GZIPInputStream gzip = new GZIPInputStream(is);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // header
            String line;
            String sql = "INSERT INTO price (price_date, ticker, price, price_std) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length != 4) continue;
                    String priceDate = parts[0].trim();
                    if (!priceDate.equals(selectedDate)) continue;
                    String ticker = parts[1].trim();
                    String price = parts[2].trim();
                    String priceStd = parts[3].trim();
                    ps.setDate(1, java.sql.Date.valueOf(priceDate));
                    ps.setString(2, ticker);
                    ps.setBigDecimal(3, new java.math.BigDecimal(price));
                    ps.setDouble(4, Double.parseDouble(priceStd));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }
} 