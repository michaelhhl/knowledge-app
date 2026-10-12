package com.knowledgeapp.qa.llm;

import java.util.Optional;

/** Empty when no provider is configured (U-01: no default provider). */
public interface LlmClientSource {

    Optional<LlmClient> client();
}
