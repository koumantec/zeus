"""Integration tests for the command queue functionality."""
from datetime import datetime

import pytest

from app.db.database import Database
from app.models.command import Command
from app.models.stack import Stack
from app.models.stack_version import ComposeDefinition, StackMetadata, StackVersion


@pytest.fixture
def db():
    """Create an in-memory database for testing."""
    database = Database()
    database.connect()
    yield database
    database.close()


def test_complete_workflow_stack_creation_and_versioning(db):
    """Test a complete workflow: create stack, add versions, enqueue commands."""
    # 1. Create a stack
    stack = Stack(
        stack_id="stack-001",
        name="my-app-stack",
        current_version=None
    )
    db.stacks.create(stack)
    
    # 2. Create initial version
    version_v1 = StackVersion(
        stack_id="stack-001",
        version="v1",
        parent_version=None,
        metadata=StackMetadata(
            created_at=datetime(2024, 1, 1, 12, 0, 0),
            created_by="admin",
            comment="Initial version"
        ),
        compose=ComposeDefinition(
            version="3.8",
            services={
                "web": {
                    "image": "nginx:latest",
                    "ports": ["80:80"]
                }
            }
        )
    )
    db.stack_versions.create(version_v1)
    
    # 3. Enqueue APPLY command for v1
    apply_cmd = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime.utcnow()
    )
    db.commands.enqueue(apply_cmd)
    
    # 4. Simulate processing the command
    next_cmd = db.commands.get_next_pending()
    assert next_cmd is not None
    assert next_cmd.type == "APPLY_STACK_VERSION"
    
    # Mark as running
    db.commands.update_status(next_cmd.command_id, "RUNNING", started_at=datetime.utcnow())
    db.command_logs.add_log(next_cmd.command_id, "INFO", "Starting to apply stack version v1")
    
    # Simulate successful completion
    db.commands.update_status(next_cmd.command_id, "DONE", ended_at=datetime.utcnow())
    db.command_logs.add_log(next_cmd.command_id, "INFO", "Stack version v1 applied successfully")
    
    # Update stack current version
    db.stacks.update_current_version("stack-001", "v1")
    
    # 5. Create a new version
    version_v2 = StackVersion(
        stack_id="stack-001",
        version="v2",
        parent_version="v1",
        metadata=StackMetadata(
            created_at=datetime(2024, 1, 2, 12, 0, 0),
            created_by="admin",
            comment="Updated nginx to alpine"
        ),
        compose=ComposeDefinition(
            version="3.8",
            services={
                "web": {
                    "image": "nginx:alpine",
                    "ports": ["80:80"]
                }
            }
        )
    )
    db.stack_versions.create(version_v2)
    
    # 6. Verify state
    stack = db.stacks.get_by_id("stack-001")
    assert stack.current_version == "v1"
    
    versions = db.stack_versions.list_by_stack("stack-001")
    assert len(versions) == 2
    
    commands = db.commands.list_by_stack("stack-001")
    assert len(commands) == 1
    assert commands[0].status == "DONE"


def test_queue_sequential_processing(db):
    """Test that commands are processed sequentially (one at a time)."""
    # Create a stack
    stack = Stack(stack_id="stack-001", name="test-stack", current_version=None)
    db.stacks.create(stack)
    
    # Enqueue multiple commands
    for i in range(5):
        cmd = Command(
            stack_id="stack-001",
            type="APPLY_STACK_VERSION",
            payload={"version": f"v{i+1}"},
            status="PENDING",
            created_at=datetime(2024, 1, 1, 12, i, 0)
        )
        db.commands.enqueue(cmd)
    
    # Simulate worker processing
    processed_count = 0
    while True:
        # Get next pending command
        next_cmd = db.commands.get_next_pending()
        if not next_cmd:
            break
        
        # Verify only one command is running at a time
        running_commands = db.commands.list_by_status("RUNNING")
        assert len(running_commands) == 0  # No other command should be running
        
        # Process the command
        db.commands.update_status(next_cmd.command_id, "RUNNING", started_at=datetime.utcnow())
        db.command_logs.add_log(
            next_cmd.command_id,
            "INFO",
            f"Processing version {next_cmd.payload['version']}"
        )
        
        # Simulate work
        processed_count += 1
        
        # Complete the command
        db.commands.update_status(next_cmd.command_id, "DONE", ended_at=datetime.utcnow())
        db.command_logs.add_log(
            next_cmd.command_id,
            "INFO",
            f"Completed version {next_cmd.payload['version']}"
        )
    
    assert processed_count == 5
    
    # Verify all commands are done
    done_commands = db.commands.list_by_status("DONE")
    assert len(done_commands) == 5


def test_command_failure_handling(db):
    """Test handling of failed commands."""
    # Create a stack
    stack = Stack(stack_id="stack-001", name="test-stack", current_version=None)
    db.stacks.create(stack)
    
    # Enqueue a command
    cmd = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime.utcnow()
    )
    db.commands.enqueue(cmd)
    
    # Process the command
    next_cmd = db.commands.get_next_pending()
    db.commands.update_status(next_cmd.command_id, "RUNNING", started_at=datetime.utcnow())
    db.command_logs.add_log(next_cmd.command_id, "INFO", "Starting command")
    
    # Simulate failure
    db.command_logs.add_log(next_cmd.command_id, "ERROR", "Failed to apply stack version")
    db.commands.update_status(next_cmd.command_id, "FAILED", ended_at=datetime.utcnow())
    
    # Verify command is marked as failed
    failed_cmd = db.commands.get_by_id(next_cmd.command_id)
    assert failed_cmd.status == "FAILED"
    
    # Verify logs contain error
    logs = db.command_logs.get_logs(next_cmd.command_id)
    error_logs = [log for log in logs if log["level"] == "ERROR"]
    assert len(error_logs) > 0


def test_multiple_stacks_independent_queues(db):
    """Test that commands for different stacks can be processed independently."""
    # Create two stacks
    stack1 = Stack(stack_id="stack-001", name="stack-1", current_version=None)
    stack2 = Stack(stack_id="stack-002", name="stack-2", current_version=None)
    db.stacks.create(stack1)
    db.stacks.create(stack2)
    
    # Enqueue commands for both stacks
    cmd1 = Command(
        stack_id="stack-001",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 12, 0, 0)
    )
    cmd2 = Command(
        stack_id="stack-002",
        type="APPLY_STACK_VERSION",
        payload={"version": "v1"},
        status="PENDING",
        created_at=datetime(2024, 1, 1, 12, 1, 0)
    )
    
    db.commands.enqueue(cmd1)
    db.commands.enqueue(cmd2)
    
    # Process commands
    next_cmd = db.commands.get_next_pending()
    assert next_cmd.stack_id == "stack-001"
    db.commands.update_status(next_cmd.command_id, "DONE")
    
    next_cmd = db.commands.get_next_pending()
    assert next_cmd.stack_id == "stack-002"
    db.commands.update_status(next_cmd.command_id, "DONE")
    
    # Verify each stack has its own commands
    stack1_commands = db.commands.list_by_stack("stack-001")
    stack2_commands = db.commands.list_by_stack("stack-002")
    
    assert len(stack1_commands) == 1
    assert len(stack2_commands) == 1
