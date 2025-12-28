"""Tests for StackRepository."""
import pytest

from app.db.database import Database
from app.models.stack import Stack


@pytest.fixture
def db():
    """Create an in-memory database for testing."""
    database = Database()
    database.connect()
    yield database
    database.close()


def test_create_stack(db):
    """Test creating a stack."""
    stack = Stack(
        stack_id="stack-001",
        name="test-stack",
        current_version=None
    )
    
    created = db.stacks.create(stack)
    
    assert created.stack_id == "stack-001"
    assert created.name == "test-stack"
    assert created.current_version is None


def test_get_stack_by_id(db):
    """Test retrieving a stack by ID."""
    stack = Stack(
        stack_id="stack-001",
        name="test-stack",
        current_version=None
    )
    db.stacks.create(stack)
    
    retrieved = db.stacks.get_by_id("stack-001")
    
    assert retrieved is not None
    assert retrieved.stack_id == "stack-001"
    assert retrieved.name == "test-stack"


def test_get_stack_by_id_not_found(db):
    """Test retrieving a non-existent stack."""
    retrieved = db.stacks.get_by_id("non-existent")
    
    assert retrieved is None


def test_get_stack_by_name(db):
    """Test retrieving a stack by name."""
    stack = Stack(
        stack_id="stack-001",
        name="test-stack",
        current_version=None
    )
    db.stacks.create(stack)
    
    retrieved = db.stacks.get_by_name("test-stack")
    
    assert retrieved is not None
    assert retrieved.stack_id == "stack-001"
    assert retrieved.name == "test-stack"


def test_list_all_stacks(db):
    """Test listing all stacks."""
    stack1 = Stack(stack_id="stack-001", name="stack-a", current_version=None)
    stack2 = Stack(stack_id="stack-002", name="stack-b", current_version=None)
    
    db.stacks.create(stack1)
    db.stacks.create(stack2)
    
    stacks = db.stacks.list_all()
    
    assert len(stacks) == 2
    assert stacks[0].name == "stack-a"
    assert stacks[1].name == "stack-b"


def test_update_current_version(db):
    """Test updating the current version of a stack."""
    stack = Stack(
        stack_id="stack-001",
        name="test-stack",
        current_version=None
    )
    db.stacks.create(stack)
    
    db.stacks.update_current_version("stack-001", "v1")
    
    retrieved = db.stacks.get_by_id("stack-001")
    assert retrieved.current_version == "v1"


def test_delete_stack(db):
    """Test deleting a stack."""
    stack = Stack(
        stack_id="stack-001",
        name="test-stack",
        current_version=None
    )
    db.stacks.create(stack)
    
    db.stacks.delete("stack-001")
    
    retrieved = db.stacks.get_by_id("stack-001")
    assert retrieved is None


def test_stack_name_unique_constraint(db):
    """Test that stack names must be unique."""
    stack1 = Stack(stack_id="stack-001", name="test-stack", current_version=None)
    stack2 = Stack(stack_id="stack-002", name="test-stack", current_version=None)
    
    db.stacks.create(stack1)
    
    with pytest.raises(Exception):  # SQLite will raise an IntegrityError
        db.stacks.create(stack2)
