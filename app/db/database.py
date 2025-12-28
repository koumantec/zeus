"""Database manager for Core Monitor."""
import sqlite3
from typing import Optional

from app.db.repositories import (
    CommandLogRepository,
    CommandRepository,
    StackRepository,
    StackVersionRepository,
)
from app.db.schema import get_connection, init_db


class Database:
    """Database manager providing access to all repositories."""
    
    def __init__(self, db_path: Optional[str] = None):
        """
        Initialize the database manager.
        
        Args:
            db_path: Path to the SQLite database file. If None, uses in-memory database.
        """
        self.db_path = db_path
        self.conn: Optional[sqlite3.Connection] = None
        
        # Repositories
        self._stack_repo: Optional[StackRepository] = None
        self._stack_version_repo: Optional[StackVersionRepository] = None
        self._command_repo: Optional[CommandRepository] = None
        self._command_log_repo: Optional[CommandLogRepository] = None
    
    def connect(self) -> None:
        """Connect to the database and initialize schema."""
        self.conn = init_db(self.db_path)
        
        # Initialize repositories
        self._stack_repo = StackRepository(self.conn)
        self._stack_version_repo = StackVersionRepository(self.conn)
        self._command_repo = CommandRepository(self.conn)
        self._command_log_repo = CommandLogRepository(self.conn)
    
    def close(self) -> None:
        """Close the database connection."""
        if self.conn:
            self.conn.close()
            self.conn = None
    
    @property
    def stacks(self) -> StackRepository:
        """Get the Stack repository."""
        if not self._stack_repo:
            raise RuntimeError("Database not connected. Call connect() first.")
        return self._stack_repo
    
    @property
    def stack_versions(self) -> StackVersionRepository:
        """Get the StackVersion repository."""
        if not self._stack_version_repo:
            raise RuntimeError("Database not connected. Call connect() first.")
        return self._stack_version_repo
    
    @property
    def commands(self) -> CommandRepository:
        """Get the Command repository."""
        if not self._command_repo:
            raise RuntimeError("Database not connected. Call connect() first.")
        return self._command_repo
    
    @property
    def command_logs(self) -> CommandLogRepository:
        """Get the CommandLog repository."""
        if not self._command_log_repo:
            raise RuntimeError("Database not connected. Call connect() first.")
        return self._command_log_repo
    
    def __enter__(self):
        """Context manager entry."""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()
