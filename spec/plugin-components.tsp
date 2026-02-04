import "@typespec/http";
import "./intellij-plugin-ui.tsp";
import "./mcp-integration.tsp";

using TypeSpec.Http;

@service({
  title: "Jakarta Migration IntelliJ Plugin Components",
  version: "1.0.0",
})
@doc("TypeSpec specification for IntelliJ plugin UI components, actions, and user interactions")
namespace JakartaMigrationPluginComponents;

@doc("Main plugin tool window that contains all migration UI components")
model MigrationToolWindow {
  @doc("Tool window identifier")
  id: string = "JakartaMigrationToolWindow";
  
  @doc("Tool window title")
  title: string = "Jakarta Migration";
  
  @doc("Current active tab")
  activeTab: ToolWindowTab;
  
  @doc("Available tabs in the tool window")
  tabs: ToolWindowTab[];
  
  @doc("Tool window state")
  state: ToolWindowState;
}

@doc("Available tabs in the migration tool window")
enum ToolWindowTab {
  @doc("Dashboard overview tab")
  Dashboard: "DASHBOARD",
  
  @doc("Dependencies analysis tab")
  Dependencies: "DEPENDENCIES",
  
  @doc("Module dependency graph tab")
  DependencyGraph: "DEPENDENCY_GRAPH",
  
  @doc("Migration phases tab")
  MigrationPhases: "MIGRATION_PHASES",
  
  @doc("Settings and configuration tab")
  Settings: "SETTINGS"
}

@doc("Tool window state")
enum ToolWindowState {
  @doc("Tool window is visible and docked")
  Visible: "VISIBLE",
  
  @doc("Tool window is hidden")
  Hidden: "HIDDEN",
  
  @doc("Tool window is floating")
  Floating: "FLOATING",
  
  @doc("Tool window is minimized")
  Minimized: "MINIMIZED"
}

@doc("Dashboard tab component showing migration overview")
model DashboardComponent {
  @doc("Dashboard data")
  dashboard: JakartaMigrationIntellijPlugin.MigrationDashboard;
  
  @doc("Available actions on the dashboard")
  actions: DashboardAction[];
  
  @doc("Refresh state")
  refreshState: RefreshState;
  
  @doc("Last update timestamp")
  lastUpdated: utcDateTime;
}

@doc("Actions available on the dashboard")
enum DashboardAction {
  @doc("Refresh analysis")
  RefreshAnalysis: "REFRESH_ANALYSIS",
  
  @doc("Start migration")
  StartMigration: "START_MIGRATION",
  
  @doc("View detailed report")
  ViewDetailedReport: "VIEW_DETAILED_REPORT",
  
  @doc("Export analysis results")
  ExportResults: "EXPORT_RESULTS"
}

@doc("Refresh state for UI components")
enum RefreshState {
  @doc("Component is idle")
  Idle: "IDLE",
  
  @doc("Component is refreshing")
  Refreshing: "REFRESHING",
  
  @doc("Refresh completed successfully")
  RefreshCompleted: "REFRESH_COMPLETED",
  
  @doc("Refresh failed")
  RefreshFailed: "REFRESH_FAILED"
}

@doc("Dependencies table component")
model DependenciesTableComponent {
  @doc("List of dependencies to display")
  dependencies: JakartaMigrationIntellijPlugin.DependencyInfo[];
  
  @doc("Table configuration")
  tableConfig: TableConfiguration;
  
  @doc("Current filter settings")
  filters: DependencyFilters;
  
  @doc("Current sort settings")
  sorting: TableSorting;
  
  @doc("Selected dependencies")
  selectedDependencies: string[];
  
  @doc("Available actions for selected dependencies")
  availableActions: DependencyAction[];
}

@doc("Table configuration settings")
model TableConfiguration {
  @doc("Visible columns")
  visibleColumns: DependencyTableColumn[];
  
  @doc("Column widths")
  columnWidths: Record<string>;
  
  @doc("Rows per page")
  rowsPerPage: int32 = 50;
  
  @doc("Enable multi-selection")
  multiSelect: boolean = true;
}

@doc("Available columns in the dependencies table")
enum DependencyTableColumn {
  @doc("Group ID column")
  GroupId: "GROUP_ID",
  
  @doc("Artifact ID column")
  ArtifactId: "ARTIFACT_ID",
  
  @doc("Current version column")
  CurrentVersion: "CURRENT_VERSION",
  
  @doc("Recommended version column")
  RecommendedVersion: "RECOMMENDED_VERSION",
  
  @doc("Migration status column")
  MigrationStatus: "MIGRATION_STATUS",
  
  @doc("Is blocker column")
  IsBlocker: "IS_BLOCKER",
  
  @doc("Risk level column")
  RiskLevel: "RISK_LEVEL",
  
  @doc("Migration impact column")
  MigrationImpact: "MIGRATION_IMPACT"
}

@doc("Filter settings for dependencies table")
model DependencyFilters {
  @doc("Filter by migration status")
  migrationStatus?: JakartaMigrationIntellijPlugin.DependencyMigrationStatus[];
  
  @doc("Show only blockers")
  showOnlyBlockers?: boolean;
  
  @doc("Filter by risk level")
  riskLevel?: JakartaMigrationIntellijPlugin.RiskLevel[];
  
  @doc("Text search filter")
  textSearch?: string;
  
  @doc("Filter by group ID")
  groupIdFilter?: string;
}

@doc("Table sorting configuration")
model TableSorting {
  @doc("Column to sort by")
  sortColumn: DependencyTableColumn;
  
  @doc("Sort direction")
  sortDirection: SortDirection;
}

@doc("Sort direction options")
enum SortDirection {
  @doc("Ascending order")
  Ascending: "ASC",
  
  @doc("Descending order")
  Descending: "DESC"
}

@doc("Actions available for dependencies")
enum DependencyAction {
  @doc("Update to recommended version")
  UpdateToRecommended: "UPDATE_TO_RECOMMENDED",
  
  @doc("View dependency details")
  ViewDetails: "VIEW_DETAILS",
  
  @doc("Find alternative dependency")
  FindAlternative: "FIND_ALTERNATIVE",
  
  @doc("Mark as reviewed")
  MarkAsReviewed: "MARK_AS_REVIEWED",
  
  @doc("Exclude from migration")
  ExcludeFromMigration: "EXCLUDE_FROM_MIGRATION"
}

@doc("Dependency graph visualization component")
model DependencyGraphComponent {
  @doc("Graph data")
  graph: JakartaMigrationIntellijPlugin.DependencyGraph;
  
  @doc("Graph layout settings")
  layout: GraphLayout;
  
  @doc("Graph interaction state")
  interactionState: GraphInteractionState;
  
  @doc("Selected nodes")
  selectedNodes: string[];
  
  @doc("Graph filters")
  filters: GraphFilters;
}

@doc("Graph layout configuration")
model GraphLayout {
  @doc("Layout algorithm")
  algorithm: GraphLayoutAlgorithm;
  
  @doc("Node spacing")
  nodeSpacing: int32 = 100;
  
  @doc("Show labels")
  showLabels: boolean = true;
  
  @doc("Highlight critical path")
  highlightCriticalPath: boolean = true;
}

@doc("Available graph layout algorithms")
enum GraphLayoutAlgorithm {
  @doc("Hierarchical layout")
  Hierarchical: "HIERARCHICAL",
  
  @doc("Force-directed layout")
  ForceDirected: "FORCE_DIRECTED",
  
  @doc("Circular layout")
  Circular: "CIRCULAR",
  
  @doc("Tree layout")
  Tree: "TREE"
}

@doc("Graph interaction state")
model GraphInteractionState {
  @doc("Current zoom level")
  zoomLevel: float64 = 1.0;
  
  @doc("Pan offset X")
  panOffsetX: float64 = 0.0;
  
  @doc("Pan offset Y")
  panOffsetY: float64 = 0.0;
  
  @doc("Interaction mode")
  mode: GraphInteractionMode;
}

@doc("Graph interaction modes")
enum GraphInteractionMode {
  @doc("Pan and zoom mode")
  PanZoom: "PAN_ZOOM",
  
  @doc("Node selection mode")
  NodeSelection: "NODE_SELECTION",
  
  @doc("Path highlighting mode")
  PathHighlighting: "PATH_HIGHLIGHTING"
}

@doc("Graph filter settings")
model GraphFilters {
  @doc("Show only modules with issues")
  showOnlyProblematic: boolean = false;
  
  @doc("Filter by migration status")
  migrationStatus?: JakartaMigrationIntellijPlugin.ModuleMigrationStatus[];
  
  @doc("Filter by risk level")
  riskLevel?: JakartaMigrationIntellijPlugin.RiskLevel[];
  
  @doc("Hide transitive dependencies")
  hideTransitiveDependencies: boolean = false;
}

@doc("Migration phases table component")
model MigrationPhasesComponent {
  @doc("List of migration phases")
  phases: JakartaMigrationIntellijPlugin.MigrationPhase[];
  
  @doc("Current active phase")
  activePhase?: string;
  
  @doc("Phase execution state")
  executionState: PhaseExecutionState;
  
  @doc("Available phase actions")
  availableActions: PhaseAction[];
  
  @doc("Progress tracking")
  progress: MigrationProgress;
}

@doc("Phase execution state")
model PhaseExecutionState {
  @doc("Currently executing phase")
  currentPhase?: string;
  
  @doc("Execution status")
  status: JakartaMigrationMcpIntegration.ExecutionStatus;
  
  @doc("Start time of current execution")
  startTime?: utcDateTime;
  
  @doc("Estimated completion time")
  estimatedCompletionTime?: utcDateTime;
}

@doc("Actions available for migration phases")
enum PhaseAction {
  @doc("Start phase execution")
  StartPhase: "START_PHASE",
  
  @doc("Pause phase execution")
  PausePhase: "PAUSE_PHASE",
  
  @doc("Skip phase")
  SkipPhase: "SKIP_PHASE",
  
  @doc("Retry failed phase")
  RetryPhase: "RETRY_PHASE",
  
  @doc("View phase details")
  ViewPhaseDetails: "VIEW_PHASE_DETAILS",
  
  @doc("Edit phase configuration")
  EditPhaseConfiguration: "EDIT_PHASE_CONFIGURATION"
}

@doc("Migration progress tracking")
model MigrationProgress {
  @doc("Overall progress percentage")
  overallProgress: float64;
  
  @doc("Current phase progress percentage")
  currentPhaseProgress: float64;
  
  @doc("Phases completed")
  phasesCompleted: int32;
  
  @doc("Total phases")
  totalPhases: int32;
  
  @doc("Tasks completed")
  tasksCompleted: int32;
  
  @doc("Total tasks")
  totalTasks: int32;
}

@doc("Plugin settings and configuration")
model PluginSettings {
  @doc("MCP server connection settings")
  mcpServerSettings: McpServerSettings;
  
  @doc("UI preferences")
  uiPreferences: UiPreferences;
  
  @doc("Analysis settings")
  analysisSettings: AnalysisSettings;
  
  @doc("Notification settings")
  notificationSettings: NotificationSettings;
}

@doc("MCP server connection settings")
model McpServerSettings {
  @doc("Server URL")
  serverUrl: string = "http://localhost:8080";
  
  @doc("Connection timeout in seconds")
  connectionTimeout: int32 = 30;
  
  @doc("Request timeout in seconds")
  requestTimeout: int32 = 300;
  
  @doc("Enable SSL verification")
  enableSslVerification: boolean = true;
  
  @doc("Authentication settings")
  authentication?: AuthenticationSettings;
}

@doc("Authentication settings for MCP server")
model AuthenticationSettings {
  @doc("Authentication type")
  type: AuthenticationType;
  
  @doc("API key (if using API key authentication)")
  apiKey?: string;
  
  @doc("Username (if using basic authentication)")
  username?: string;
  
  @doc("Password (if using basic authentication)")
  password?: string;
}

@doc("Authentication types")
enum AuthenticationType {
  @doc("No authentication")
  None: "NONE",
  
  @doc("API key authentication")
  ApiKey: "API_KEY",
  
  @doc("Basic authentication")
  Basic: "BASIC",
  
  @doc("Bearer token authentication")
  Bearer: "BEARER"
}

@doc("UI preferences")
model UiPreferences {
  @doc("Default tool window tab")
  defaultTab: ToolWindowTab = ToolWindowTab.Dashboard;
  
  @doc("Auto-refresh interval in seconds")
  autoRefreshInterval: int32 = 300;
  
  @doc("Enable auto-refresh")
  enableAutoRefresh: boolean = true;
  
  @doc("Theme preferences")
  theme: UiTheme = UiTheme.System;
  
  @doc("Show tooltips")
  showTooltips: boolean = true;
}

@doc("UI theme options")
enum UiTheme {
  @doc("Follow system theme")
  System: "SYSTEM",
  
  @doc("Light theme")
  Light: "LIGHT",
  
  @doc("Dark theme")
  Dark: "DARK"
}

@doc("Analysis settings")
model AnalysisSettings {
  @doc("Include test dependencies in analysis")
  includeTestDependencies: boolean = true;
  
  @doc("Include transitive dependencies")
  includeTransitiveDependencies: boolean = true;
  
  @doc("Enable source code scanning")
  enableSourceCodeScanning: boolean = true;
  
  @doc("Analysis timeout in seconds")
  analysisTimeout: int32 = 600;
  
  @doc("Cache analysis results")
  cacheResults: boolean = true;
  
  @doc("Cache duration in hours")
  cacheDurationHours: int32 = 24;
}

@doc("Notification settings")
model NotificationSettings {
  @doc("Enable desktop notifications")
  enableDesktopNotifications: boolean = true;
  
  @doc("Show analysis completion notifications")
  showAnalysisCompletionNotifications: boolean = true;
  
  @doc("Show migration progress notifications")
  showMigrationProgressNotifications: boolean = true;
  
  @doc("Show error notifications")
  showErrorNotifications: boolean = true;
  
  @doc("Notification sound")
  enableNotificationSound: boolean = false;
}

@doc("Plugin action definitions")
model PluginAction {
  @doc("Action identifier")
  id: string;
  
  @doc("Action name")
  name: string;
  
  @doc("Action description")
  description?: string;
  
  @doc("Action category")
  category: ActionCategory;
  
  @doc("Keyboard shortcut")
  shortcut?: string;
  
  @doc("Action availability")
  availability: ActionAvailability;
}

@doc("Action categories")
enum ActionCategory {
  @doc("Analysis actions")
  Analysis: "ANALYSIS",
  
  @doc("Migration actions")
  Migration: "MIGRATION",
  
  @doc("View actions")
  View: "VIEW",
  
  @doc("Settings actions")
  Settings: "SETTINGS",
  
  @doc("Help actions")
  Help: "HELP"
}

@doc("Action availability conditions")
model ActionAvailability {
  @doc("Requires project to be open")
  requiresProject: boolean = true;
  
  @doc("Requires MCP server connection")
  requiresMcpConnection: boolean = true;
  
  @doc("Requires analysis to be completed")
  requiresAnalysis: boolean = false;
  
  @doc("Requires migration plan")
  requiresMigrationPlan: boolean = false;
}

@doc("User interaction events")
model UserInteractionEvent {
  @doc("Event type")
  eventType: InteractionEventType;
  
  @doc("Event timestamp")
  timestamp: utcDateTime;
  
  @doc("Component that generated the event")
  component: string;
  
  @doc("Event data")
  data?: Record<unknown>;
  
  @doc("User context")
  userContext?: UserContext;
}

@doc("Types of user interaction events")
enum InteractionEventType {
  @doc("Button click")
  ButtonClick: "BUTTON_CLICK",
  
  @doc("Table row selection")
  TableRowSelection: "TABLE_ROW_SELECTION",
  
  @doc("Graph node selection")
  GraphNodeSelection: "GRAPH_NODE_SELECTION",
  
  @doc("Filter change")
  FilterChange: "FILTER_CHANGE",
  
  @doc("Sort change")
  SortChange: "SORT_CHANGE",
  
  @doc("Tab switch")
  TabSwitch: "TAB_SWITCH",
  
  @doc("Settings change")
  SettingsChange: "SETTINGS_CHANGE"
}

@doc("User context information")
model UserContext {
  @doc("Current project path")
  projectPath?: string;
  
  @doc("Active file")
  activeFile?: string;
  
  @doc("Selected text")
  selectedText?: string;
  
  @doc("Cursor position")
  cursorPosition?: CursorPosition;
}

@doc("Cursor position in editor")
model CursorPosition {
  @doc("Line number (0-based)")
  line: int32;
  
  @doc("Column number (0-based)")
  column: int32;
}

@doc("Plugin state management")
model PluginState {
  @doc("Current plugin version")
  version: string;
  
  @doc("Plugin initialization state")
  initializationState: InitializationState;
  
  @doc("MCP server connection state")
  mcpConnectionState: ConnectionState;
  
  @doc("Last analysis results")
  lastAnalysisResults?: JakartaMigrationMcpIntegration.AnalyzeMigrationImpactResponse;
  
  @doc("Current migration plan")
  currentMigrationPlan?: JakartaMigrationMcpIntegration.MigrationPlan;
  
  @doc("Plugin settings")
  settings: PluginSettings;
}

@doc("Plugin initialization states")
enum InitializationState {
  @doc("Plugin not initialized")
  NotInitialized: "NOT_INITIALIZED",
  
  @doc("Plugin initializing")
  Initializing: "INITIALIZING",
  
  @doc("Plugin initialized successfully")
  Initialized: "INITIALIZED",
  
  @doc("Plugin initialization failed")
  InitializationFailed: "INITIALIZATION_FAILED"
}

@doc("MCP server connection states")
enum ConnectionState {
  @doc("Not connected")
  Disconnected: "DISCONNECTED",
  
  @doc("Connecting")
  Connecting: "CONNECTING",
  
  @doc("Connected")
  Connected: "CONNECTED",
  
  @doc("Connection failed")
  ConnectionFailed: "CONNECTION_FAILED",
  
  @doc("Connection lost")
  ConnectionLost: "CONNECTION_LOST"
}