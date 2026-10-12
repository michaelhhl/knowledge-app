package com.knowledgeapp.qa.llm;

import java.util.List;

/** Provider-neutral chat completion. Implementations must not log the API key. */
public interface LlmClient {

    /** Returns the assistant message text. Throws {@link LlmException} on transport or provider errors. */
    String complete(List<LlmMessage> messages);
}
