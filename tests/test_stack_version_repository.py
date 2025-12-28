"""Tests for StackVersionRepository."""
from datetime import datetime

import pytest

from app.db.database import Database
from app.models.stack import Stack
from app.models.stack_version import (
    ComposeDefinition,
    RuntimeMapping,
    StackMetadata,
    StackStatus,
    StackVersion,
)


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
def sample_stack_version():
    """Create a sample stack version for testing."""
    return StackVersion(
        stack_id="stack-001",
        version="v1",
        parent_version=None,
        metadata=StackMetadata(
            created_at=datetime(2024, 1, 1, 12, 0, 0),
            created_by="test-user",
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


def test_create_stack_version(db, sample_stack_version):
    """Test creating a stack version."""
    created = db.stack_versions.create(sample_stack_version)
    
    assert created.stack_id == "stack-001"
    assert created.version == "v1"
    assert created.parent_version is None
    assert created.metadata.created_by == "test-user"


def test_get_stack_version_by_version(db, sample_stack_version):
    """Test retrieving a stack version."""
    db.stack_versions.create(sample_stack_version)
    
    retrieved = db.stack_versions.get_by_version("stack-001", "v1")
    
    assert retrieved is not None
    assert retrieved.stack_id == "stack-001"
    assert retrieved.version == "v1"
    assert retrieved.metadata.created_by == "test-user"
    assert "web" in retrieved.compose.services


def test_get_stack_version_not_found(db):
    """Test retrieving a non-existent stack version."""
    retrieved = db.stack_versions.get_by_version("stack-001", "v999")
    
    assert retrieved is None


def test_list_versions_by_stack(db, sample_stack_version):
    """Test listing all versions of a stack."""
    # Create multiple versions
    v1 = sample_stack_version
    db.stack_versions.create(v1)
    
    v2 = StackVersion(
        stack_id="stack-001",
        version="v2",
        parent_version="v1",
        metadata=StackMetadata(
            created_at=datetime(2024, 1, 2, 12, 0, 0),
            created_by="test-user",
            comment="Second version"
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
    db.stack_versions.create(v2)
    
    versions = db.stack_versions.list_by_stack("stack-001")
    
    assert len(versions) == 2
    # Should be ordered by created_at DESC
    assert versions[0].version == "v2"
    assert versions[1].version == "v1"


def test_update_runtime_mapping(db, sample_stack_version):
    """Test updating the runtime mapping of a stack version."""
    db.stack_versions.create(sample_stack_version)
    
    runtime_mapping = {
        "networks": {"default": "net_abc123"},
        "volumes": {},
        "containers": {
            "web": {"id": "container_xyz789", "name": "stack-001_web"}
        }
    }
    
    db.stack_versions.update_runtime("stack-001", "v1", runtime_mapping)
    
    retrieved = db.stack_versions.get_by_version("stack-001", "v1")
    assert retrieved.runtime is not None
    assert retrieved.runtime.containers["web"]["id"] == "container_xyz789"


def test_update_status(db, sample_stack_version):
    """Test updating the status of a stack version."""
    db.stack_versions.create(sample_stack_version)
    
    status = {
        "desired": "RUNNING",
        "actual": "RUNNING",
        "last_updated": datetime(2024, 1, 1, 13, 0, 0).isoformat()
    }
    
    db.stack_versions.update_status("stack-001", "v1", status)
    
    retrieved = db.stack_versions.get_by_version("stack-001", "v1")
    assert retrieved.status is not None
    assert retrieved.status.desired == "RUNNING"
    assert retrieved.status.actual == "RUNNING"


def test_stack_version_with_runtime_and_status(db):
    """Test creating a stack version with runtime and status."""
    stack_version = StackVersion(
        stack_id="stack-001",
        version="v1",
        parent_version=None,
        metadata=StackMetadata(
            created_at=datetime(2024, 1, 1, 12, 0, 0),
            created_by="test-user",
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
        ),
        runtime=RuntimeMapping(
            networks={"default": "net_abc123"},
            volumes={},
            containers={"web": {"id": "container_xyz789"}}
        ),
        status=StackStatus(
            desired="RUNNING",
            actual="RUNNING",
            last_updated=datetime(2024, 1, 1, 13, 0, 0)
        )
    )
    
    created = db.stack_versions.create(stack_version)
    retrieved = db.stack_versions.get_by_version("stack-001", "v1")
    
    assert retrieved.runtime is not None
    assert retrieved.runtime.networks["default"] == "net_abc123"
    assert retrieved.status is not None
    assert retrieved.status.desired == "RUNNING"


def test_cascade_delete_versions_with_stack(db, sample_stack_version):
    """Test that deleting a stack cascades to its versions."""
    db.stack_versions.create(sample_stack_version)
    
    # Verify version exists
    retrieved = db.stack_versions.get_by_version("stack-001", "v1")
    assert retrieved is not None
    
    # Delete the stack
    db.stacks.delete("stack-001")
    
    # Verify version is also deleted
    retrieved = db.stack_versions.get_by_version("stack-001", "v1")
    assert retrieved is None
