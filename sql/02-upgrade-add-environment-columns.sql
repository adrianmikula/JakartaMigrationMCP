-- Jakarta Migration Plugin - Supabase Schema Upgrade
-- Adds environment column to usage_events and error_reports tables
-- Run this script to upgrade existing schemas to support dev/demo/prod separation

-- =============================================================================
-- UPGRADE: ADD ENVIRONMENT COLUMN TO USAGE_EVENTS TABLE
-- =============================================================================
ALTER TABLE usage_events 
ADD COLUMN environment VARCHAR(20) DEFAULT 'prod';

-- Add constraint to limit environment values (only applies when environment is not NULL)
ALTER TABLE usage_events 
ADD CONSTRAINT chk_usage_events_environment 
CHECK (environment IS NULL OR environment IN ('dev', 'demo', 'prod'));

-- Create index for environment-based queries
CREATE INDEX idx_usage_events_environment ON usage_events(environment);

-- =============================================================================
-- UPGRADE: ADD ENVIRONMENT COLUMN TO ERROR_REPORTS TABLE
-- =============================================================================
ALTER TABLE error_reports 
ADD COLUMN environment VARCHAR(20) DEFAULT 'prod';

-- Add constraint to limit environment values (only applies when environment is not NULL)
ALTER TABLE error_reports 
ADD CONSTRAINT chk_error_reports_environment 
CHECK (environment IS NULL OR environment IN ('dev', 'demo', 'prod'));

-- Create index for environment-based queries
CREATE INDEX idx_error_reports_environment ON error_reports(environment);

-- =============================================================================
-- SAMPLE QUERIES WITH ENVIRONMENT FILTERING
-- =============================================================================

-- Get usage statistics by environment
SELECT 
    environment,
    event_type,
    COUNT(*) as event_count,
    DATE_TRUNC('day', created_at) as event_date
FROM usage_events 
GROUP BY environment, event_type, DATE_TRUNC('day', created_at)
ORDER BY environment, event_date DESC;

-- Get top error types by environment
SELECT 
    environment,
    error_type,
    COUNT(*) as error_count,
    plugin_version
FROM error_reports 
GROUP BY environment, error_type, plugin_version
ORDER BY environment, error_count DESC;

-- Get activity summary by user_id and environment
SELECT 
    environment,
    user_id,
    MIN(created_at) as first_event,
    MAX(created_at) as last_event,
    COUNT(DISTINCT id) as usage_events_count,
    COUNT(DISTINCT id) as error_reports_count
FROM (
    SELECT environment, user_id, id, created_at FROM usage_events
    UNION ALL
    SELECT environment, user_id, id, created_at FROM error_reports
) combined
GROUP BY environment, user_id
ORDER BY environment, last_event DESC;

-- Get production-only usage by trigger action
SELECT 
    trigger_action,
    COUNT(*) as usage_count,
    DATE_TRUNC('day', created_at) as usage_date
FROM usage_events 
WHERE trigger_action IS NOT NULL 
  AND environment = 'prod'
GROUP BY trigger_action, DATE_TRUNC('day', created_at)
ORDER BY usage_date DESC;

-- =============================================================================
-- MIGRATION NOTES
-- =============================================================================
-- 1. All existing records will default to 'prod' environment
-- 2. New plugin versions should specify environment when inserting records
-- 3. Old plugin versions will continue to work and default to 'prod'
-- 4. The environment column defaults to 'prod' for backwards compatibility
