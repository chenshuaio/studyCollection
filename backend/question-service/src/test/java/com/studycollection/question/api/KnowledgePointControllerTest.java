package com.studycollection.question.api;

import com.studycollection.question.app.InMemoryKnowledgePointRepository;
import com.studycollection.question.domain.KnowledgePoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgePointControllerTest {
    @Test
    void adminCanListCreateAndDisableKnowledgePoints() {
        KnowledgePointController controller = new KnowledgePointController(new InMemoryKnowledgePointRepository());

        KnowledgePoint created = controller.create(new CreateKnowledgePointRequest("JVM", "虚拟机运行时内存和类加载")).data();

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("JVM");
        assertThat(created.enabled()).isTrue();
        assertThat(controller.list().data()).extracting(KnowledgePoint::name).contains("JVM");

        KnowledgePoint disabled = controller.disable(created.id()).data();

        assertThat(disabled.enabled()).isFalse();
        assertThat(controller.list().data())
                .filteredOn(point -> point.id().equals(created.id()))
                .singleElement()
                .extracting(KnowledgePoint::enabled)
                .isEqualTo(false);
    }
}
