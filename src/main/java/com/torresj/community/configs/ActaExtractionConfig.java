package com.torresj.community.configs;

import com.torresj.community.services.ActaExtractionService;
import com.torresj.community.services.impl.ClaudeActaExtractionService;
import com.torresj.community.services.impl.NoOpActaExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the acta extractor: the Claude vision implementation when an API key is present,
 * otherwise a no-op that keeps imports working offline (tests, local without a key).
 */
@Configuration
@Slf4j
public class ActaExtractionConfig {

    @Bean
    ActaExtractionService actaExtractionService(@Value("${ANTHROPIC_API_KEY:}") String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("Claude API key detected; enabling PDF auto-extraction.");
            return new ClaudeActaExtractionService(apiKey);
        }
        log.info("No Claude API key; PDF auto-extraction disabled (imports create empty drafts).");
        return new NoOpActaExtractionService();
    }
}
