package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
public class DependencyTreeResult {
    private final List<DependencyNode> dependencies;
    private final Set<String> scopes;
    private final boolean success;
    private final String errorMessage;

    public DependencyTreeResult(List<DependencyNode> deps, Set<String> scopes) {
        this.dependencies = deps != null ? deps : Collections.emptyList();
        this.scopes = scopes != null ? scopes : Collections.emptySet();
        this.success = true;
        this.errorMessage = null;
    }

    private DependencyTreeResult(String error) {
        this.dependencies = Collections.emptyList();
        this.scopes = Collections.emptySet();
        this.success = false;
        this.errorMessage = error;
    }

    public static DependencyTreeResult error(String msg) { return new DependencyTreeResult(msg); }
    public static DependencyTreeResult empty() { return new DependencyTreeResult(Collections.emptyList(), Collections.emptySet()); }

    @Getter
    public static class DependencyNode {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String scope;
        private final int depth;
        private final boolean transitive;

        public DependencyNode(String g, String a, String v, String s, int d, boolean t) {
            this.groupId = g; this.artifactId = a; this.version = v; this.scope = s; this.depth = d; this.transitive = t;
        }

        public String getArtifactKey() { return groupId + ":" + artifactId; }
    }
}
