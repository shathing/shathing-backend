package com.shathing.backend.config;

import com.shathing.backend.repository.LegalDongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.legal-dong", name = "initialize-on-startup", havingValue = "true")
public class LegalDongCsvLoader implements ApplicationRunner {

    private static final int BATCH_SIZE = 1000;

    private final LegalDongRepository legalDongRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.legal-dong.csv-path}")
    private String csvPath;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (legalDongRepository.count() > 0) {
            log.info("Skip legal dong CSV load because table already contains data.");
            return;
        }

        ClassPathResource resource = new ClassPathResource(csvPath);
        List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);
        int insertedCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length < 8) {
                    throw new IllegalStateException("Unexpected CSV format: " + line);
                }

                String code = columns[0].trim();
                String sidoName = columns[1].trim();
                String sigunguName = columns[2].trim();
                String eupMyeonDongName = columns[3].trim();
                String riName = columns[4].trim();
                String deletedAt = columns[7].trim();

                if (eupMyeonDongName.isEmpty() || !riName.isEmpty() || !deletedAt.isEmpty()) {
                    continue;
                }

                batchArgs.add(new Object[]{code, sidoName, emptyToNull(sigunguName), eupMyeonDongName});

                if (batchArgs.size() == BATCH_SIZE) {
                    insertedCount += flushBatch(batchArgs);
                }
            }
        }

        if (!batchArgs.isEmpty()) {
            insertedCount += flushBatch(batchArgs);
        }

        log.info("Loaded {} legal dong rows from {}.", insertedCount, csvPath);
    }

    private int flushBatch(List<Object[]> batchArgs) {
        jdbcTemplate.batchUpdate(
                "insert into legal_dong (code, sido_name, sigungu_name, eup_myeon_dong_name) values (?, ?, ?, ?)",
                batchArgs
        );
        int batchSize = batchArgs.size();
        batchArgs.clear();
        return batchSize;
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
