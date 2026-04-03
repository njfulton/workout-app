-- Processing log for every email handled by the worker
CREATE TABLE IF NOT EXISTS processing_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  sender_name TEXT NOT NULL,
  sender_address TEXT NOT NULL,
  subject TEXT NOT NULL,
  recipient TEXT NOT NULL,
  destination TEXT NOT NULL,  -- 'notes', 'capture', 'tasks'
  database_id TEXT NOT NULL,
  tag TEXT,
  format TEXT,
  notion_page_id TEXT,
  status TEXT NOT NULL DEFAULT 'success',  -- 'success' or 'error'
  error_message TEXT,
  raw_email BLOB  -- stored for retry capability
);

-- Configuration table for runtime-editable settings
CREATE TABLE IF NOT EXISTS config (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_log_created_at ON processing_log(created_at);
CREATE INDEX IF NOT EXISTS idx_log_status ON processing_log(status);
CREATE INDEX IF NOT EXISTS idx_log_destination ON processing_log(destination);

-- Seed default config
INSERT OR IGNORE INTO config (key, value) VALUES
  ('tag_mappings', '{"pm":"Product Management","ai":"AI","biz":"Business","tech":"Technology","fin":"Finance","pol":"Politics","lead":"Leadership","inv":"Investing","geo":"Geopolitics"}'),
  ('notes_defaults', '{"format":"Article","function":["Consume"],"status":"To Read","inputOutput":"Input"}'),
  ('capture_defaults', '{"status":"Inbox"}'),
  ('tasks_defaults', '{"status":"Inbox"}'),
  ('special_tags', '{"newsletter":{"format":"Newsletter"}}');
