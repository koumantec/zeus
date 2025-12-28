"""Tests for CommandLogRepository."""
from datetime import datetime

import pytest

from app.db.database import Database
from app.models.command import Command
from app.models.stack import Stack


@pytest.fixture
def db():
    """Create an in-memory database for testing."""
    database = Database()
    database.connect()
    
    # Create a stack and command for testing
    stack = Stack(stack_id="stack-001", name="test-stack", current_version=None)
    database.stacks.create(stack)
    
    command = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 12, 0, 0)
    )
    database.commands.enqueue(command)
    
    yield database
    database.close()


def test_add_log(db):
    """Test adding a log entry for a command."""
    command = db.commands.get_next_pending()
    
    db.command_logs.add_log(
        command_id=command.command_id,
        level="INFO",
        message="Command started",
        timestamp=datetime(2024, 1, 1, 12, 0, 0)
    )
    
    logs = db.command_logs.get_logs(command.command_id)
    
    assert len(logs) == 1
    assert logs[0]["level"] == "INFO"
    assert logs[0]["message"] == "Command started"


def test_add_multiple_logs(db):
    """Test adding multiple log entries for a command."""
    command = db.commands.get_next_pending()
    
    db.command_logs.add_log(
        command_id=command.command_id,
        level="INFO",
        message="Command started",
        timestamp=datetime(2024, 1, 1, 12, 0, 0)
    )
    db.command_logs.add_log(
        command_id=command.command_id,
        level="INFO",
        message="Processing...",
        timestamp=datetime(2024, 1, 1, 12, 1, 0)
    )
    db.command_logs.add_log(
        command_id=command.command_id,
        level="INFO",
        message="Command completed",
        timestamp=datetime(2024, 1, 1, 12, 2, 0)
    )
    
    logs = db.command_logs.get_logs(command.command_id)
    
    assert len(logs) == 3
    assert logs[0]["message"] == "Command started"
    assert logs[1]["message"] == "Processing..."
    assert logs[2]["message"] == "Command completed"


def test_add_log_with_different_levels(db):
    """Test adding logs with different severity levels."""
    command = db.commands.get_next_pending()
    
    db.command_logs.add_log(command.command_id, "INFO", "Info message")
    db.command_logs.add_log(command.command_id, "WARNING", "Warning message")
    db.command_logs.add_log(command.command_id, "ERROR", "Error message")
    
    logs = db.command_logs.get_logs(command.command_id)
    
    assert len(logs) == 3
    levels = [log["level"] for log in logs]
    assert "INFO" in levels
    assert "WARNING" in levels
    assert "ERROR" in levels


def test_get_logs_empty(db):
    """Test getting logs for a command with no logs."""
    command = db.commands.get_next_pending()
    
    logs = db.command_logs.get_logs(command.command_id)
    
    assert len(logs) == 0


def test_logs_ordered_by_timestamp(db):
    """Test that logs are returned in chronological order."""
    command = db.commands.get_next_pending()
    
    # Add logs in non-chronological order
    db.command_logs.add_log(
        command.command_id, "INFO", "Third",
        timestamp=datetime(2024, 1, 1, 12, 2, 0)
    )
    db.command_logs.add_log(
        command.command_id, "INFO", "First",
        timestamp=datetime(2024, 1, 1, 12, 0, 0)
    )
    db.command_logs.add_log(
        command.command_id, "INFO", "Second",
        timestamp=datetime(2024, 1, 1, 12, 1, 0)
    )
    
    logs = db.command_logs.get_logs(command.command_id)
    
    assert logs[0]["message"] == "First"
    assert logs[1]["message"] == "Second"
    assert logs[2]["message"] == "Third"


def test_cascade_delete_logs_with_command(db):
    """Test that deleting a command cascades to its logs."""
    command = db.commands.get_next_pending()
    
    db.command_logs.add_log(command.command_id, "INFO", "Test log")
    
    # Verify log exists
    logs = db.command_logs.get_logs(command.command_id)
    assert len(logs) == 1
    
    # Delete the stack (which cascades to commands and logs)
    db.stacks.delete("stack-001")
    
    # Verify log is also deleted
    logs = db.command_logs.get_logs(command.command_id)
    assert len(logs) == 0
