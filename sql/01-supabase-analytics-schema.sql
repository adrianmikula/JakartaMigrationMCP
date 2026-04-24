-- Jakarta Migration Plugin - Supabase Analytics Schema (Simplified)
-- This file contains SQL schema for tracking usage metrics and error reports without user table dependency

-- =============================================================================
-- USAGE EVENTS TABLE
-- Tracks user interactions with plugin using UUID directly without foreign key constraints
-- =============================================================================
CREATE TABLE usage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL, -- Direct UUID without foreign key constraint
    event_type VARCHAR(50) NOT NULL, -- 'credit_used', 'upgrade_clicked'
    current_ui_tab VARCHAR(100), -- Currently active UI tab when event occurred
    plugin_version VARCHAR(50), -- Plugin version when event occurred
    trigger_action VARCHAR(100), -- Action that triggered the event
    event_data JSONB, -- Additional event-specific data (e.g., upgrade source)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_event_type CHECK (event_type IN ('credit_used', 'upgrade_clicked'))
);

-- Create indexes for faster queries
CREATE INDEX idx_usage_events_user_id ON usage_events(user_id);
CREATE INDEX idx_usage_events_created_at ON usage_events(created_at);
CREATE INDEX idx_usage_events_event_type ON usage_events(event_type);
CREATE INDEX idx_usage_events_current_ui_tab ON usage_events(current_ui_tab);
CREATE INDEX idx_usage_events_plugin_version ON usage_events(plugin_version);
CREATE INDEX idx_usage_events_trigger_action ON usage_events(trigger_action);

-- =============================================================================
-- ERROR REPORTS TABLE
-- Collects error information for debugging and improvement without user table dependency
-- =============================================================================
CREATE TABLE error_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL, -- Direct UUID without foreign key constraint
    plugin_version VARCHAR(50) NOT NULL,
    current_tab VARCHAR(100), -- Currently active UI tab when error occurred
    error_type VARCHAR(100) NOT NULL, -- Exception class name
    error_message TEXT, -- Error message from exception
    stack_trace TEXT, -- Full stack trace (plugin code only)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for faster queries
CREATE INDEX idx_error_reports_user_id ON error_reports(user_id);
CREATE INDEX idx_error_reports_created_at ON error_reports(created_at);
CREATE INDEX idx_error_reports_error_type ON error_reports(error_type);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Enable anonymous access for analytics data
-- =============================================================================
ALTER TABLE usage_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE error_reports ENABLE ROW LEVEL SECURITY;

-- Allow anonymous inserts and reads
CREATE POLICY "Allow anonymous insert" ON usage_events FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow anonymous insert" ON error_reports FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow anonymous read" ON usage_events FOR SELECT USING (true);
CREATE POLICY "Allow anonymous read" ON error_reports FOR SELECT USING (true);

-- =============================================================================
-- SAMPLE QUERIES (Updated for simplified schema)
-- =============================================================================

-- Get usage statistics by event type
SELECT 
    event_type,
    COUNT(*) as event_count,
    DATE_TRUNC('day', created_at) as event_date
FROM usage_events 
GROUP BY event_type, DATE_TRUNC('day', created_at)
ORDER BY event_date DESC;

-- Get top error types
SELECT 
    error_type,
    COUNT(*) as error_count,
    plugin_version
FROM error_reports 
GROUP BY error_type, plugin_version
ORDER BY error_count DESC
LIMIT 10;

-- Get activity summary by user_id (simplified)
SELECT 
    user_id,
    MIN(created_at) as first_event,
    MAX(created_at) as last_event,
    COUNT(DISTINCT id) as usage_events_count,
    COUNT(DISTINCT id) as error_reports_count
FROM (
    SELECT user_id, id, created_at FROM usage_events
    UNION ALL
    SELECT user_id, id, created_at FROM error_reports
) combined
GROUP BY user_id
ORDER BY last_event DESC;

-- Get usage by trigger action
SELECT 
    trigger_action,
    COUNT(*) as usage_count,
    DATE_TRUNC('day', created_at) as usage_date
FROM usage_events 
WHERE trigger_action IS NOT NULL
GROUP BY trigger_action, DATE_TRUNC('day', created_at)
ORDER BY usage_date DESC;
