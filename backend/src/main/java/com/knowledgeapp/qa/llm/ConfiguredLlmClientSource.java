package com.knowledgeapp.qa.llm;

import com.knowledgeapp.config.AppProperties;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ConfiguredLlmClientSource implements LlmClientSource {

    public static final String PROVIDER_OPENAI_COMPATIBLE = "openai-compatible";

    private static final Logger log = LoggerFactory.getLogger(ConfiguredLlmClientSource.class);

    private final Optional<LlmClient> client;

    public ConfiguredLlmClientSource(AppProperties properties) {
        this.client = build(properties.llm());
    }

    @Override
    public Optional<LlmClient> client() {
        return client;
    }

    private static Optional<LlmClient> build(AppProperties.Llm llm) {
        if (llm == null
                || !StringUtils.hasText(llm.provider())
                || !StringUtils.hasText(llm.model())
                || !StringUtils.hasText(llm.apiKey())
                || !StringUtils.hasText(llm.baseUrl())) {
            log.info("LLM not configured; Q&A disabled, browsing and retrieval remain available");
            return Optional.empty();
        }
        String provider = llm.provider().strip().toLowerCase(Locale.ROOT);
        if (!PROVIDER_OPENAI_COMPATIBLE.equals(provider)) {
            log.warn("Unsupported LLM_PROVIDER '{}'; Q&A disabled", provider);
            return Optional.empty();
        }
        log.info("LLM provider {} with model {}", provider, llm.model());
        return Optional.of(new OpenAiCompatibleLlmClient(
                llm.baseUrl().strip(), llm.model().strip(), llm.apiKey().strip(), llm.timeout()));
    }
}
