package com.knowledgeapp.qa;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgeapp.qa.QaDtos.HistoryTurn;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryPlannerTest {

    private final QueryPlanner planner = new QueryPlanner();
    private final List<HistoryTurn> history = List.of(new HistoryTurn("韩国签证照片有什么要求", "白底…"));

    @Test
    void keepsStandaloneQuestion() {
        assertThat(planner.plan("住民票需要什么条件", history)).isEqualTo("住民票需要什么条件");
        assertThat(planner.plan("这个要多久", List.of())).isEqualTo("这个要多久");
    }

    @Test
    void prependsPreviousQuestionForFollowUps() {
        assertThat(planner.plan("这个要彩色吗", history)).isEqualTo("韩国签证照片有什么要求 这个要彩色吗");
        assertThat(planner.plan("上一份呢", history)).startsWith("韩国签证照片有什么要求");
        assertThat(planner.plan("尺寸？", history)).startsWith("韩国签证照片有什么要求");
    }
}
