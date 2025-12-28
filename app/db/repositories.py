"""Repository layer for database operations."""
import json
import sqlite3
from datetime import datetime
from typing import List, Optional

from app.models.command import Command
from app.models.stack import Stack
from app.models.stack_version import StackVersion


class StackRepository:
    """Repository for Stack operations."""
    
    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn
    
    def create(self, stack: Stack) -> Stack:
        """Create a new stack."""
        cursor = self.conn.cursor()
        now = datetime.utcnow().isoformat()
        
        cursor.execute(
            """
            INSERT INTO stacks (stack_id, name, current_version, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            (stack.stack_id, stack.name, stack.current_version, now, now)
        )
        self.conn.commit()
        return stack
    
    def get_by_id(self, stack_id: str) -> Optional[Stack]:
        """Get a stack by ID."""
        cursor = self.conn.cursor()
        cursor.execute(
            "SELECT stack_id, name, current_version FROM stacks WHERE stack_id = ?",
            (stack_id,)
        )
        row = cursor.fetchone()
        
        if row:
            return Stack(
                stack_id=row["stack_id"],
                name=row["name"],
                current_version=row["current_version"]
            )
        return None
    
    def get_by_name(self, name: str) -> Optional[Stack]:
        """Get a stack by name."""
        cursor = self.conn.cursor()
        cursor.execute(
            "SELECT stack_id, name, current_version FROM stacks WHERE name = ?",
            (name,)
        )
        row = cursor.fetchone()
        
        if row:
            return Stack(
                stack_id=row["stack_id"],
                name=row["name"],
                current_version=row["current_version"]
            )
        return None
    
    def list_all(self) -> List[Stack]:
        """List all stacks."""
        cursor = self.conn.cursor()
        cursor.execute("SELECT stack_id, name, current_version FROM stacks ORDER BY name")
        rows = cursor.fetchall()
        
        return [
            Stack(
                stack_id=row["stack_id"],
                name=row["name"],
                current_version=row["current_version"]
            )
            for row in rows
        ]
    
    def update_current_version(self, stack_id: str, version: str) -> None:
        """Update the current version of a stack."""
        cursor = self.conn.cursor()
        now = datetime.utcnow().isoformat()
        
        cursor.execute(
            "UPDATE stacks SET current_version = ?, updated_at = ? WHERE stack_id = ?",
            (version, now, stack_id)
        )
        self.conn.commit()
    
    def delete(self, stack_id: str) -> None:
        """Delete a stack (cascade deletes versions and commands)."""
        cursor = self.conn.cursor()
        cursor.execute("DELETE FROM stacks WHERE stack_id = ?", (stack_id,))
        self.conn.commit()


class StackVersionRepository:
    """Repository for StackVersion operations."""
    
    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn
    
    def create(self, stack_version: StackVersion) -> StackVersion:
        """Create a new stack version."""
        cursor = self.conn.cursor()
        
        # Serialize compose definition
        compose_json = stack_version.compose.model_dump_json()
        
        # Serialize runtime mapping if present
        runtime_json = None
        if stack_version.runtime:
            runtime_json = stack_version.runtime.model_dump_json()
        
        # Serialize status if present
        status_json = None
        if stack_version.status:
            status_json = stack_version.status.model_dump_json()
        
        cursor.execute(
            """
            INSERT INTO stack_versions 
            (stack_id, version, parent_version, created_at, created_by, comment,
             compose_definition, runtime_mapping, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                stack_version.stack_id,
                stack_version.version,
                stack_version.parent_version,
                stack_version.metadata.created_at.isoformat(),
                stack_version.metadata.created_by,
                stack_version.metadata.comment,
                compose_json,
                runtime_json,
                status_json
            )
        )
        self.conn.commit()
        return stack_version
    
    def get_by_version(self, stack_id: str, version: str) -> Optional[StackVersion]:
        """Get a specific version of a stack."""
        cursor = self.conn.cursor()
        cursor.execute(
            """
            SELECT stack_id, version, parent_version, created_at, created_by, comment,
                   compose_definition, runtime_mapping, status
            FROM stack_versions
            WHERE stack_id = ? AND version = ?
            """,
            (stack_id, version)
        )
        row = cursor.fetchone()
        
        if row:
            return self._row_to_stack_version(row)
        return None
    
    def list_by_stack(self, stack_id: str) -> List[StackVersion]:
        """List all versions of a stack."""
        cursor = self.conn.cursor()
        cursor.execute(
            """
            SELECT stack_id, version, parent_version, created_at, created_by, comment,
                   compose_definition, runtime_mapping, status
            FROM stack_versions
            WHERE stack_id = ?
            ORDER BY created_at DESC
            """,
            (stack_id,)
        )
        rows = cursor.fetchall()
        
        return [self._row_to_stack_version(row) for row in rows]
    
    def update_runtime(self, stack_id: str, version: str, runtime_mapping: dict) -> None:
        """Update the runtime mapping of a stack version."""
        cursor = self.conn.cursor()
        runtime_json = json.dumps(runtime_mapping)
        
        cursor.execute(
            """
            UPDATE stack_versions 
            SET runtime_mapping = ?
            WHERE stack_id = ? AND version = ?
            """,
            (runtime_json, stack_id, version)
        )
        self.conn.commit()
    
    def update_status(self, stack_id: str, version: str, status: dict) -> None:
        """Update the status of a stack version."""
        cursor = self.conn.cursor()
        status_json = json.dumps(status)
        
        cursor.execute(
            """
            UPDATE stack_versions 
            SET status = ?
            WHERE stack_id = ? AND version = ?
            """,
            (status_json, stack_id, version)
        )
        self.conn.commit()
    
    def _row_to_stack_version(self, row: sqlite3.Row) -> StackVersion:
        """Convert a database row to a StackVersion object."""
        from app.models.stack_version import (
            ComposeDefinition,
            RuntimeMapping,
            StackMetadata,
            StackStatus,
        )
        
        # Parse compose definition
        compose_data = json.loads(row["compose_definition"])
        compose = ComposeDefinition(**compose_data)
        
        # Parse metadata
        metadata = StackMetadata(
            created_at=datetime.fromisoformat(row["created_at"]),
            created_by=row["created_by"],
            comment=row["comment"]
        )
        
        # Parse runtime mapping if present
        runtime = None
        if row["runtime_mapping"]:
            runtime_data = json.loads(row["runtime_mapping"])
            runtime = RuntimeMapping(**runtime_data)
        
        # Parse status if present
        status = None
        if row["status"]:
            status_data = json.loads(row["status"])
            status = StackStatus(**status_data)
        
        return StackVersion(
            stack_id=row["stack_id"],
            version=row["version"],
            parent_version=row["parent_version"],
            metadata=metadata,
            compose=compose,
            runtime=runtime,
            status=status
        )


class CommandRepository:
    """Repository for Command operations."""
    
    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn
    
    def enqueue(self, command: Command) -> Command:
        """Enqueue a new command."""
        cursor = self.conn.cursor()
        
        payload_json = json.dumps(command.payload)
        
        cursor.execute(
            """
            INSERT INTO commands 
            (stack_id, type, payload, status, created_at, started_at, ended_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (
                command.stack_id,
                command.type,
                payload_json,
                command.status,
                command.created_at.isoformat(),
                command.started_at.isoformat() if command.started_at else None,
                command.ended_at.isoformat() if command.ended_at else None
            )
        )
        self.conn.commit()
        
        # Get the generated command_id
        command.command_id = cursor.lastrowid
        return command
    
    def get_by_id(self, command_id: int) -> Optional[Command]:
        """Get a command by ID."""
        cursor = self.conn.cursor()
        cursor.execute(
            """
            SELECT command_id, stack_id, type, payload, status, 
                   created_at, started_at, ended_at
            FROM commands
            WHERE command_id = ?
            """,
            (command_id,)
        )
        row = cursor.fetchone()
        
        if row:
            return self._row_to_command(row)
        return None
    
    def list_by_stack(self, stack_id: str) -> List[Command]:
        """List all commands for a stack."""
        cursor = self.conn.cursor()
        cursor.execute(
            """
            SELECT command_id, stack_id, type, payload, status, 
                   created_at, started_at, ended_at
            FROM commands
            WHERE stack_id = ?
            ORDER BY created_at DESC
            """,
            (stack_id,)
        )
        rows = cursor.fetchall()
        
        return [self._row_to_command(row) for row in rows]
    
    def list_by_status(self, status: str) -> List[Command]:
        """List all commands with a specific status."""
        cursor = self.conn.cursor()
        cursor.execute(
            """
            SELECT command_id, stack_id, type, payload, status, 
                   created_at, started_at, ended_at
            FROM commands
            WHERE status = ?
            ORDER BY created_at ASC
            """,
            (status,)
        )
        rows = cursor.fetchall()
        
        return [self._row_to_command(row) for row in rows]
    
    def get_next_pending(self) -> Optional[Command]:
        """Get the next pending command (oldest first)."""
        cursor = self.conn.cursor()
        cursor.execute(
            """
            SELECT command_id, stack_id, type, payload, status, 
                   created_at, started_at, ended_at
            FROM commands
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT 1
            """)
        row = cursor.fetchone()
        
        if row:
            return self._row_to_command(row)
        return None
    
    def update_status(
        self,
        command_id: int,
        status: str,
        started_at: Optional[datetime] = None,
        ended_at: Optional[datetime] = None
    ) -> None:
        """Update the status of a command."""
        cursor = self.conn.cursor()
        
        cursor.execute(
            """
            UPDATE commands 
            SET status = ?, started_at = ?, ended_at = ?
            WHERE command_id = ?
            """,
            (
                status,
                started_at.isoformat() if started_at else None,
                ended_at.isoformat() if ended_at else None,
                command_id
            )
        )
        self.conn.commit()
    
    def _row_to_command(self, row: sqlite3.Row) -> Command:
        """Convert a database row to a Command object."""
        payload = json.loads(row["payload"])
        
        return Command(
            command_id=row["command_id"],
            stack_id=row["stack_id"],
            type=row["type"],
            payload=payload,
            status=row["status"],
            created_at=datetime.fromisoformat(row["created_at"]),
            started_at=datetime.fromisoformat(row["started_at"]) if row["started_at"] else None,
            ended_at=datetime.fromisoformat(row["ended_at"]) if row["ended_at"] else None
        )


class CommandLogRepository:
    """Repository for CommandLog operations."""
    
    def __init__(self, conn: sqlite3.Connection):
        self.conn = conn
    
    def add_log(
        self,
        command_id: int,
        level: str,
        message: str,
        timestamp: Optional[datetime] = None
    ) -> None:
        """Add a log entry for a command."""
        cursor = self.conn.cursor()
        
        if timestamp is None:
            timestamp = datetime.utcnow()
        
        cursor.execute(
            """
            INSERT INTO command_logs (command_id, timestamp, level, message)
            VALUES (?, ?, ?, ?)
            """,
            (command_id, timestamp.isoformat(), level, message)
        )
        self.conn.commit()
    
    def get_logs(self, command_id: int) -> List[dict]:
        """Get all logs for a command."""
        cursor = self.conn.cursor()
        cursor.execute(
            """
            SELECT log_id, command_id, timestamp, level, message
            FROM command_logs
            WHERE command_id = ?
            ORDER BY timestamp ASC
            """,
            (command_id,)
        )
        rows = cursor.fetchall()
        
        return [
            {
                "log_id": row["log_id"],
                "command_id": row["command_id"],
                "timestamp": row["timestamp"],
                "level": row["level"],
                "message": row["message"]
            }
            for row in rows
        ]
