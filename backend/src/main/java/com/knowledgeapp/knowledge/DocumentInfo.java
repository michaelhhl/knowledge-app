package com.knowledgeapp.knowledge;

import java.util.List;

public record DocumentInfo(String path, String id, String title, List<String> tags, String updated) {
}
