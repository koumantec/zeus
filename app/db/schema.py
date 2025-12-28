"""Database schema initialization for SQLite."""
import sqlite3
from pathlib import Path
from typing import Optional


def init_db(db_path: Optional[str] = None) -> sqlite3.Connection:
    """
    Initialize the database schema.
    
    Args:
        db_path: Path to the SQLite database file. If None, uses in-memory database.
    
    Returns:
        sqlite3.Connection: Database connection
    """
    if db_path:
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        conn = sqlite3.connect(db_path, check_same_thread=False)
    else:
        conn = sqlite3.connect(":memory:", check_same_thread=False)
    
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    # Table: stacks
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS stacks (
            stack_id TEXT PRIMARY KEY,
            name TEXT NOT NULL UNIQUE,
            current_version TEXT,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        )
    """)
    
    # Table: stack_versions
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS stack_versions (
            stack_id TEXT NOT NULL,
            version TEXT NOT NULL,
            parent_version TEXT,
            
            -- Metadata
            created_at TEXT NOT NULL,
            created_by TEXT NOT NULL,
            comment TEXT,
            
            -- Compose definition (stored as JSON)
            compose_definition TEXT NOT NULL,
            
            -- Runtime mapping (stored as JSON, nullable)
            runtime_mapping TEXT,
            
            -- Status (stored as JSON, nullable)
            status TEXT,
            
            PRIMARY KEY (stack_id, version),
            FOREIGN KEY (stack_id) REFERENCES stacks(stack_id) ON DELETE CASCADE
        )
    """)
    
    # Index for querying versions by stack
    cursor.execute("""
        CREATE INDEX IF NOT EXISTS idx_stack_versions_stack_id 
        ON stack_versions(stack_id)
    """)
    
    # Table: commands
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS commands (
            command_id INTEGER PRIMARY KEY AUTOINCREMENT,
            stack_id TEXT NOT NULL,
            type TEXT NOT NULL,
            payload TEXT NOT NULL,
            
            status TEXT NOT NULL DEFAULT 'PENDING',
            
            created_at TEXT NOT NULL,
            started_at TEXT,
            ended_at TEXT,
            
            FOREIGN KEY (stack_id) REFERENCES stacks(stack_id) ON DELETE CASCADE
        )
    """)
    
    # Index for querying commands by stack
    cursor.execute("""
        CREATE INDEX IF NOT EXISTS idx_commands_stack_id 
        ON commands(stack_id)
    """)
    
    # Index for querying commands by status
    cursor.execute("""
        CREATE INDEX IF NOT EXISTS idx_commands_status 
        ON commands(status)
    """)
    
    # Index for ordering commands (queue behavior)
    cursor.execute("""
        CREATE INDEX IF NOT EXISTS idx_commands_created_at 
        ON commands(created_at)
    """)
    
    # Table: command_logs
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS command_logs (
            log_id INTEGER PRIMARY KEY AUTOINCREMENT,
            command_id INTEGER NOT NULL,
            timestamp TEXT NOT NULL,
            level TEXT NOT NULL,
            message TEXT NOT NULL,
            
            FOREIGN KEY (command_id) REFERENCES commands(command_id) ON DELETE CASCADE
        )
    """)
    
    # Index for querying logs by command
    cursor.execute("""
        CREATE INDEX IF NOT EXISTS idx_command_logs_command_id 
        ON command_logs(command_id)
    """)
    
    conn.commit()
    return conn


def get_connection(db_path: Optional[str] = None) -> sqlite3.Connection:
    """
    Get a database connection.
    
    Args:
        db_path: Path to the SQLite database file. If None, uses in-memory database.
    
    Returns:
        sqlite3.Connection: Database connection
    """
    if db_path:
        conn = sqlite3.connect(db_path, check_same_thread=False)
    else:
        conn = sqlite3.connect(":memory:", check_same_thread=False)
    
    conn.row_factory = sqlite3.Row
    return conn
