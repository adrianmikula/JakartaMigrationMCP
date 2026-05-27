# Jakarta Migration MCP - Project Roadmap

A high-level overview of the project's strategic direction and planned features.

---

## 🎯 Product Vision

**Risk radar for AI-assisted Jakarta EE migration**

Position the plugin as a tool that catches what developers might miss and helps them prioritize migration work—rather than claiming to fully automate refactoring.

---

## 📋 Strategic Pillars

### 1. Trust & Transparency
Build confidence with senior developers through:
- Clear signal-to-noise ratio in scan results
- Transparent limitations and confidence scores
- Explainable risk assessments

**Key deliverables:**
- Risk scoring with confidence metrics
- Clear indication of scan limitations
- Human-readable reasoning for all recommendations

### 2. Deep Analysis
Move beyond surface-level detection to comprehensive migration analysis:

| Feature | Status | Description |
|---------|--------|-------------|
| Deep JAR Scanning | ✅ Complete | Bytecode analysis of dependencies to detect javax/jakarta usage |
| Transitive Dependency Analysis | ✅ Complete | Full dependency tree scanning with incompatibility propagation |
| Test Coverage Integration | 📝 Planned | Add validation confidence as 4th risk dimension |
| Reflection Usage Detection | ✅ Complete | Detect dynamic class loading and reflection calls |

### 3. Actionable Insights
Convert analysis into clear action items:

- **Risk Dashboard** - Visual risk assessment with 4 dimensions
  - Code Risk
  - Infrastructure Risk  
  - Organisational Complexity
  - Validation Confidence (Test Coverage)

- **Dependency Reports** - HTML reports with professional layout
- **Migration Strategy** - Tailored recommendations based on risk profile

---

## 🗺️ Feature Roadmap

### Phase 1: Core Analysis (Completed)
- ✅ Bytecode-level JAR scanning
- ✅ Transitive dependency tree analysis
- ✅ Maven/Gradle build file parsing
- ✅ Platform detection (Tomcat, JBoss, WebSphere, etc.)

### Phase 2: Enhanced Reporting (In Progress)
- 🔄 HTML report generation (replaced PDF)
- 🔄 Risk score visualization
- 📝 Professional report templates
- 📝 Executive summary generation

### Phase 3: Intelligence & Trust (Planned)
- 📝 Test coverage integration
- 📝 Critical risk zone detection (high risk + low coverage)
- 📝 Migration effort estimation
- 📝 Recipe recommendations linked to scan results

### Phase 4: Developer Experience (Planned)
- 📝 Improved dependency table with filtering
- 📝 Dependency graph visualization
- 📝 Real-time scan progress
- 📝 IDE integration refinements

---

## 📁 Detailed Specifications

For detailed implementation specifications, see:

- `docs/roadmap/conversion_ui_features.md` - UI improvements and test coverage integration
- `docs/roadmap/deep_jar_scanning.md` - JAR bytecode analysis design
- `docs/roadmap/redesign_pdf_reports.md` - Report generation (now HTML)
- `docs/roadmap/senior_dev_trust.md` - Trust and positioning strategy
- `docs/roadmap/dependency_tree_view.md` - Dependency visualization
- `docs/roadmap/virtual_threads_performance.md` - Performance optimizations
- `docs/roadmap/ci_static_analysis.md` - Static analysis integration

---

## 🔧 Technical Architecture

### Modules
- **community-core-engine** - Core analysis engine (open source)
- **community-mcp-server** - MCP server implementation
- **premium-core-engine** - Advanced scanning features
- **premium-intellij-plugin** - IDE integration
- **premium-mcp-server** - Premium MCP tools

### Key Technologies
- ASM for bytecode analysis
- Maven/Gradle toolchains for dependency resolution
- HTML/CSS for report generation
- IntelliJ Platform SDK for plugin development

---

## 📊 Success Metrics

- **Accuracy**: False positive rate < 10%
- **Coverage**: Support 95%+ of Jakarta EE migration scenarios
- **Performance**: Scan completes in < 30 seconds for typical projects
- **Adoption**: Senior developer validation and trust

---

*Last updated: May 2026*
