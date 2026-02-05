// TypeSpec specification for Jakarta Migration Premium MCP Tools
// JetBrains Marketplace licensed features
// 
// This file documents the premium MCP tools API that requires
// a JetBrains Marketplace license ($49/mo or $399/yr)

import "./intellij-plugin-ui.tsp";

namespace JakartaMigrationMcpIntegration.Premium;

// Note: This namespace contains premium-only MCP tools
// These tools are only available in the premium-engine module
// Community builds return upgrade_required responses

// === Premium Tool Definitions ===

// @doc("Create a comprehensive migration plan - PREMIUM feature")
// model CreateMigrationPlanRequest {
//   projectPath: string;
//   strategy?: MigrationStrategy;
// }

// @doc("Analyze full migration impact - PREMIUM feature")
// model AnalyzeMigrationImpactRequest {
//   projectPath: string;
// }

// @doc("Verify runtime execution of migrated application - PREMIUM feature")
// model VerifyRuntimeRequest {
//   jarPath: string;
//   timeoutSeconds?: int32 = 30;
// }

// @doc("Apply automatic fixes to source files - PREMIUM feature")
// model ApplyAutoFixesRequest {
//   projectPath: string;
//   files?: string[];
//   dryRun?: boolean = false;
// }

// @doc("Execute a complete migration plan in phases - PREMIUM feature")
// model ExecuteMigrationPlanRequest {
//   projectPath: string;
// }

// === Response Models ===

// @doc("Response with migration plan - PREMIUM")
// model CreateMigrationPlanResponse {
//   status: string;
//   edition: string = "premium";
//   phaseCount: int32;
//   estimatedDuration: string;
//   riskScore: int32;
//   riskFactors: string[];
//   prerequisites: string[];
//   phases: PremiumMigrationPhase[];
// }

// @doc("Premium migration phase")
// model PremiumMigrationPhase {
//   number: int32;
//   description: string;
//   fileCount: int32;
//   estimatedDuration: string;
// }

// === License Check ===

// @doc("Upgrade required response for premium features")
// model UpgradeRequiredResponse {
//   status: string = "upgrade_required";
//   tier: string = "PREMIUM";
//   feature: string;
//   pricing: PricingInfo;
//   purchaseUrl: string;
//   message: string;
// }

// @doc("Pricing information for premium license")
// model PricingInfo {
//   monthly: int32;
//   yearly: int32;
//   currency: string = "USD";
// }
