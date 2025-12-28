"""Tests for CommandRepository and command queue functionality."""
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
    
    # Create a stack for testing
    stack = Stack(stack_id="stack-001", name="test-stack", current_version=None)
    database.stacks.create(stack)
    
    yield database
    database.close()


@pytest.fixture
def sample_command():
    """Create a sample command for testing."""
    return Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 12, 0, 0)
    )


def test_enqueue_command(db, sample_command):
    """Test enqueuing a command."""
    enqueued = db.commands.enqueue(sample_command)
    
    assert enqueued.command_id is not None
    assert enqueued.stack_id == "stack-001"
    assert enqueued.type == "APPLY_STACK_VERSION"
    assert enqueued.status == "PENDING"


def test_get_command_by_id(db, sample_command):
    """Test retrieving a command by ID."""
    enqueued = db.commands.enqueue(sample_command)
    
    retrieved = db.commands.get_by_id(enqueued.command_id)
    
    assert retrieved is not None
    assert retrieved.command_id == enqueued.command_id
    assert retrieved.stack_id == "stack-001"
    assert retrieved.type == "APPLY_STACK_VERSION"


def test_get_command_not_found(db):
    """Test retrieving a non-existent command."""
    retrieved = db.commands.get_by_id(999)
    
    assert retrieved is None


def test_list_commands_by_stack(db):
    """Test listing all commands for a stack."""
    cmd1 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 12, 0, 0)
    )
    cmd2 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v2"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 13, 0, 0)
    )
    
    db.commands.enqueue(cmd1)
    db.commands.enqueue(cmd2)
    
    commands = db.commands.list_by_stack("stack-001")
    
    assert len(commands) == 2
    # Should be ordered by created_at DESC
    assert commands[0].payload["version"] == "v2"
    assert commands[1].payload["version"] == "v1"


def test_list_commands_by_status(db):
    """Test listing commands by status."""
    cmd1 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 12, 0, 0)
    )
    cmd2 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v2"},
        status="DONE",
        created_at=datetime(2024, 1, 1, 13, 0, 0)
    )
    
    db.commands.enqueue(cmd1)
    db.commands.enqueue(cmd2)
    
    pending = db.commands.list_by_status("PENDING")
    done = db.commands.list_by_status("DONE")
    
    assert len(pending) == 1
    assert pending[0].payload["version"] == "v1"
    assert len(done) == 1
    assert done[0].payload["version"] == "v2"


def test_get_next_pending_command(db):
    """Test getting the next pending command (queue behavior)."""
    # Enqueue multiple commands
    cmd1 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 12, 0, 0)
    )
    cmd2 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v2"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 13, 0, 0)
    )
    cmd3 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v3"},
        status="RUNNING",
        created_at=datetime(2024, 1, 1, 14, 0, 0)
    )
    
    db.commands.enqueue(cmd1)
    db.commands.enqueue(cmd2)
    db.commands.enqueue(cmd3)
    
    # Should get the oldest pending command
    next_cmd = db.commands.get_next_pending()
    
    assert next_cmd is not None
    assert next_cmd.payload["version"] == "v1"
    assert next_cmd.status == "PENDING"


def test_get_next_pending_no_pending_commands(db):
    """Test getting next pending when no pending commands exist."""
    cmd = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="DONE",
        created_at=datetime(2024, 1, 1, 12, 0, 0)
    )
    db.commands.enqueue(cmd)
    
    next_cmd = db.commands.get_next_pending()
    
    assert next_cmd is None


def test_update_command_status(db, sample_command):
    """Test updating the status of a command."""
    enqueued = db.commands.enqueue(sample_command)
    
    started_at = datetime(2024, 1, 1, 12, 5, 0)
    db.commands.update_status(enqueued.command_id, "RUNNING", started_at=started_at)
    
    retrieved = db.commands.get_by_id(enqueued.command_id)
    assert retrieved.status == "RUNNING"
    assert retrieved.started_at == started_at
    
    ended_at = datetime(2024, 1, 1, 12, 10, 0)
    db.commands.update_status(enqueued.command_id, "DONE", ended_at=ended_at)
    
    retrieved = db.commands.get_by_id(enqueued.command_id)
    assert retrieved.status == "DONE"
    assert retrieved.ended_at == ended_at


def test_command_queue_ordering(db):
    """Test that commands are processed in FIFO order."""
    # Enqueue commands in specific order
    for i in range(5):
        cmd = Command(
            stack_id="stack-001",
            type="APPLY_STACK_VERSION",
            payload={"version": f"v{i+1}"},
            status="PENDING",
            created_at=datetime(2024, 1, 1, 12, i, 0)
        )
        db.commands.enqueue(cmd)
    
    # Process commands one by one
    processed_versions = []
    while True:
        next_cmd = db.commands.get_next_pending()
        if not next_cmd:
            break
        
        processed_versions.append(next_cmd.payload["version"])
        db.commands.update_status(next_cmd.command_id, "DONE")
    
    # Verify FIFO order
    assert processed_versions == ["v1", "v2", "v3", "v4", "v5"]


def test_cascade_delete_commands_with_stack(db, sample_command):
    """Test that deleting a stack cascades to its commands."""
    enqueued = db.commands.enqueue(sample_command)
    
    # Verify command exists
    retrieved = db.commands.get_by_id(enqueued.command_id)
    assert retrieved is not None
    
    # Delete the stack
    db.stacks.delete("stack-001")
    
    # Verify command is also deleted
    retrieved = db.commands.get_by_id(enqueued.command_id)
    assert retrieved is None
