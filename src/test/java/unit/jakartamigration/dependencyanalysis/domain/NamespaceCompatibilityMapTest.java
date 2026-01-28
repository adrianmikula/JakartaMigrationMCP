package unit.jakartamigration.dependencyanalysis.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.domain.NamespaceCompatibilityMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NamespaceCompatibilityMap Tests")
class NamespaceCompatibilityMapTest {

    @Test
    void shouldPutAndGet() {
        NamespaceCompatibilityMap map = new NamespaceCompatibilityMap();
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        map.put(a, Namespace.JAKARTA);
        assertThat(map.get(a)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    void shouldReturnUnknownForMissingKey() {
        NamespaceCompatibilityMap map = new NamespaceCompatibilityMap();
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        assertThat(map.get(a)).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    void shouldExposeGetAll() {
        NamespaceCompatibilityMap map = new NamespaceCompatibilityMap();
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        map.put(a, Namespace.JAVAX);
        Map<Artifact, Namespace> all = map.getAll();
        assertThat(all).containsEntry(a, Namespace.JAVAX);
        assertThat(all).isNotSameAs(map.getAll());
    }

    @Test
    void shouldReportContainsKey() {
        NamespaceCompatibilityMap map = new NamespaceCompatibilityMap();
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        assertThat(map.containsKey(a)).isFalse();
        map.put(a, Namespace.JAKARTA);
        assertThat(map.containsKey(a)).isTrue();
    }

    @Test
    void shouldReportSize() {
        NamespaceCompatibilityMap map = new NamespaceCompatibilityMap();
        assertThat(map.size()).isZero();
        map.put(new Artifact("g", "a", "1.0", "compile", false), Namespace.JAKARTA);
        assertThat(map.size()).isOne();
    }

    @Test
    void shouldAcceptMapInConstructor() {
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        Map<Artifact, Namespace> initial = Map.of(a, Namespace.JAVAX);
        NamespaceCompatibilityMap map = new NamespaceCompatibilityMap(initial);
        assertThat(map.get(a)).isEqualTo(Namespace.JAVAX);
        assertThat(map.size()).isOne();
    }
}
