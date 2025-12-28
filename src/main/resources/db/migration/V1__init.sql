PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA foreign_keys=ON;
PRAGMA busy_timeout=5000;

CREATE TABLE IF NOT EXISTS stacks (
  stack_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  current_version TEXT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS stack_versions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  stack_id TEXT NOT NULL,
  version TEXT NOT NULL,
  parent_version TEXT NULL,
  body_json TEXT NOT NULL,
  created_at TEXT NOT NULL,
  created_by TEXT NOT NULL,
  comment TEXT NULL,
  UNIQUE(stack_id, version),
  FOREIGN KEY(stack_id) REFERENCES stacks(stack_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS commands (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  stack_id TEXT NOT NULL,
  type TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  status TEXT NOT NULL,  -- PENDING/RUNNING/DONE/FAILED/CANCELLED
  created_at TEXT NOT NULL,
  started_at TEXT NULL,
  ended_at TEXT NULL,
  error_message TEXT NULL,
  FOREIGN KEY(stack_id) REFERENCES stacks(stack_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_commands_status_id ON commands(status, id);
CREATE INDEX IF NOT EXISTS idx_commands_stack ON commands(stack_id);

CREATE TABLE IF NOT EXISTS command_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  command_id INTEGER NOT NULL,
  ts TEXT NOT NULL,
  level TEXT NOT NULL,
  message TEXT NOT NULL,
  FOREIGN KEY(command_id) REFERENCES commands(id) ON DELETE CASCADE
);
