-- Jakarta Migration Plugin - Supabase Analytics Schema
-- This file contains the SQL schema for tracking usage metrics and error reports

-- =============================================================================
-- USERS TABLE
-- Stores anonymous user identification and metadata
-- =============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anonymous_id VARCHAR(255) UNIQUE NOT NULL,
    plugin_version VARCHAR(50) NOT NULL,
    first_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for faster lookups by anonymous_id
CREATE INDEX idx_users_anonymous_id ON users(anonymous_id);

-- =============================================================================
-- USAGE EVENTS TABLE
-- Tracks user interactions with the plugin
-- =============================================================================
CREATE TABLE usage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL, -- 'credit_used', 'upgrade_clicked'
    credit_type VARCHAR(50), -- 'basic_scan', 'advanced_scan', 'pdf_report', 'refactor'
    event_data JSONB, -- Additional event-specific data (e.g., upgrade source)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_event_type CHECK (event_type IN ('credit_used', 'upgrade_clicked'))
);

-- Create indexes for faster queries
CREATE INDEX idx_usage_events_user_id ON usage_events(user_id);
CREATE INDEX idx_usage_events_created_at ON usage_events(created_at);
CREATE INDEX idx_usage_events_event_type ON usage_events(event_type);

-- =============================================================================
-- ERROR REPORTS TABLE
-- Collects error information for debugging and improvement
-- =============================================================================
CREATE TABLE error_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
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
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE error_reports ENABLE ROW LEVEL SECURITY;

-- Allow anonymous inserts (for new users and events)
CREATE POLICY "Allow anonymous insert" ON users FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow anonymous insert" ON usage_events FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow anonymous insert" ON error_reports FOR INSERT WITH CHECK (true);

-- Allow anonymous reads (for analytics queries)
CREATE POLICY "Allow anonymous read" ON users FOR SELECT USING (true);
CREATE POLICY "Allow anonymous read" ON usage_events FOR SELECT USING (true);
CREATE POLICY "Allow anonymous read" ON error_reports FOR SELECT USING (true);

-- =============================================================================
-- SAMPLE QUERIES
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

-- Get user activity summary
SELECT 
    u.anonymous_id,
    u.first_seen,
    u.last_seen,
    COUNT(DISTINCT ue.id) as usage_events_count,
    COUNT(DISTINCT er.id) as error_reports_count
FROM users u
LEFT JOIN usage_events ue ON u.id = ue.user_id
LEFT JOIN error_reports er ON u.id = er.user_id
GROUP BY u.id, u.anonymous_id, u.first_seen, u.last_seen
ORDER BY u.last_seen DESC;
