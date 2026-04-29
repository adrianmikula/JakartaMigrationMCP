package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import lombok.extern.slf4j.Slf4j;

/**
 * Dependency graph snippet showing interactive force-directed visualization.
 * Uses Vis.js library for scalable, interactive graph rendering.
 */
@Slf4j
public class DependencyGraphSnippet extends BaseHtmlSnippet {
    
    private final DependencyGraph dependencyGraph;
    
    public DependencyGraphSnippet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }
    
    @Override
    public String generate() throws SnippetGenerationException {
        if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
            return generateNoDependenciesMessage();
        }
        
        return safelyFormat("""
            <div class="section">
                <h2>Dependency Graph Visualization</h2>
                <p>Interactive force-directed graph showing all dependencies and their relationships. 
                Drag nodes to rearrange, scroll to zoom, hover for details.</p>
                
                <div id="dependency-graph-container" style="height: 600px; border: 1px solid #e1e8ed; border-radius: 8px; background: #fafbfc;"></div>
                
                <div class="graph-legend" style="margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px; border: 1px solid #e1e8ed;">
                    <h4 style="margin: 0 0 10px 0; color: #2c3e50;">Legend</h4>
                    <div style="display: flex; gap: 20px; flex-wrap: wrap;">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <div style="width: 16px; height: 16px; background: #28a745; border-radius: 50%; border: 2px solid #1e7e34;"></div>
                            <span style="color: #333;">Jakarta Compatible</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <div style="width: 16px; height: 16px; background: #ffc107; border-radius: 50%; border: 2px solid #d39e00;"></div>
                            <span style="color: #333;">Needs Update</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <div style="width: 16px; height: 16px; background: #dc3545; border-radius: 50%; border: 2px solid #c82333;"></div>
                            <span style="color: #333;">No Jakarta Version</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <div style="width: 16px; height: 16px; background: #6c757d; border-radius: 50%; border: 2px solid #545b62;"></div>
                            <span style="color: #333;">Unknown Status</span>
                        </div>
                    </div>
                </div>
                
                <script type="text/javascript">
                    // Dependency graph data
                    var graphNodes = %s;
                    var graphEdges = %s;
                    
                    // Create network
                    var container = document.getElementById('dependency-graph-container');
                    var data = {
                        nodes: new vis.DataSet(graphNodes),
                        edges: new vis.DataSet(graphEdges)
                    };
                    
                    var options = {
                        nodes: {
                            shape: 'box',
                            font: {
                                size: 14,
                                face: 'Segoe UI, Tahoma, Geneva, Verdana, sans-serif'
                            },
                            margin: 10,
                            widthConstraint: {
                                maximum: 200
                            }
                        },
                        edges: {
                            arrows: 'to',
                            smooth: {
                                type: 'continuous'
                            },
                            color: {
                                color: '#848484',
                                highlight: '#3498db'
                            }
                        },
                        physics: {
                            forceAtlas2Based: {
                                gravitationalConstant: -50,
                                centralGravity: 0.01,
                                springLength: 100,
                                springConstant: 0.08
                            },
                            maxVelocity: 50,
                            solver: 'forceAtlas2Based',
                            timestep: 0.35,
                            stabilization: {
                                iterations: 150
                            }
                        },
                        interaction: {
                            hover: true,
                            tooltipDelay: 200,
                            zoomView: true,
                            dragView: true
                        }
                    };
                    
                    var network = new vis.Network(container, data, options);
                    
                    // Fit graph to container after stabilization
                    network.once('stabilizationIterationsDone', function() {
                        network.fit();
                    });
                </script>
            </div>
            """,
            generateNodesJson(),
            generateEdgesJson()
        );
    }
    
    private String generateNodesJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        int count = 0;
        for (Artifact artifact : dependencyGraph.getNodes()) {
            if (count > 0) {
                json.append(",\n");
            }
            
            String id = escapeJson(artifact.toIdentifier());
            String label = escapeJson(truncateLabel(artifact.artifactId()));
            String color = getNodeColor(artifact);
            String title = escapeJson(generateNodeTitle(artifact));
            
            json.append(String.format("    {\"id\": \"%s\", \"label\": \"%s\", \"color\": %s, \"title\": \"%s\"}",
                id, label, color, title));
            
            count++;
        }
        
        json.append("\n  ]");
        return json.toString();
    }
    
    private String generateEdgesJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        int count = 0;
        for (Dependency edge : dependencyGraph.getEdges()) {
            if (count > 0) {
                json.append(",\n");
            }
            
            String from = escapeJson(edge.from().toIdentifier());
            String to = escapeJson(edge.to().toIdentifier());
            
            json.append(String.format("    {\"from\": \"%s\", \"to\": \"%s\"}", from, to));
            
            count++;
        }
        
        json.append("\n  ]");
        return json.toString();
    }
    
    private String getNodeColor(Artifact artifact) {
        String colorHex;
        String borderHex;
        
        if (artifact.isJakartaCompatible()) {
            colorHex = "#28a745";
            borderHex = "#1e7e34";
        } else if (artifact.groupId().startsWith("javax.") || 
                   artifact.groupId().contains("spring") ||
                   artifact.artifactId().contains("javax")) {
            colorHex = "#ffc107";
            borderHex = "#d39e00";
        } else if (artifact.groupId().contains("legacy") || 
                   artifact.artifactId().contains("old") ||
                   artifact.version().matches("^[0-4]\\.")) {
            colorHex = "#dc3545";
            borderHex = "#c82333";
        } else {
            colorHex = "#6c757d";
            borderHex = "#545b62";
        }
        
        return String.format("{\"background\": \"%s\", \"border\": \"%s\"}", colorHex, borderHex);
    }
    
    private String generateNodeTitle(Artifact artifact) {
        return String.format("%s:%s:%s (scope: %s, transitive: %s)",
            artifact.groupId(),
            artifact.artifactId(),
            artifact.version(),
            artifact.scope(),
            artifact.transitive());
    }
    
    private String truncateLabel(String label) {
        if (label == null) return "unknown";
        if (label.length() <= 30) return label;
        return label.substring(0, 27) + "...";
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    private String generateNoDependenciesMessage() {
        return """
            <div class="section">
                <h2>Dependency Graph Visualization</h2>
                <div class="warning-box" style="background: #fff3cd; border: 1px solid #ffeaa7; border-left: 4px solid #f39c12; padding: 15px; margin: 10px 0; border-radius: 4px;">
                    <h3 style="color: #856404; margin: 0 0 10px 0;">⚠️ No Dependencies Found</h3>
                    <p style="margin: 0; color: #856404;">Unable to generate dependency graph. This might indicate an Eclipse project without Maven/Gradle build files, or missing dependency configuration.</p>
                </div>
            </div>
            """;
    }
    
    @Override
    public boolean isApplicable() {
        return dependencyGraph != null;
    }
    
    @Override
    public int getOrder() {
        return 35; // Show after metrics, before dependency matrix
    }
}
