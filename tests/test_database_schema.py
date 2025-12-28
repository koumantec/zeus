"""Tests for database schema initialization."""
import sqlite3

import pytest

from app.db.schema import init_db


def test_init_db_creates_tables():
    """Test that init_db creates all required tables."""
    conn = init_db()
    cursor = conn.cursor()
    
    # Check that all tables exist
    cursor.execute(
        "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
    )
    tables = [row[0] for row in cursor.fetchall()]
    
    assert "stacks" in tables
    assert "stack_versions" in tables
    assert "commands" in tables
    assert "command_logs" in tables
    
    conn.close()


def test_init_db_creates_indexes():
    """Test that init_db creates all required indexes."""
    conn = init_db()
    cursor = conn.cursor()
    
    # Check that all indexes exist
    cursor.execute(
        "SELECT name FROM sqlite_master WHERE type='index' ORDER BY name"
    )
    indexes = [row[0] for row in cursor.fetchall()]
    
    assert "idx_stack_versions_stack_id" in indexes
    assert "idx_commands_stack_id" in indexes
    assert "idx_commands_status" in indexes
    assert "idx_commands_created_at" in indexes
    assert "idx_command_logs_command_id" in indexes
    
    conn.close()


def test_stacks_table_schema():
    """Test the schema of the stacks table."""
    conn = init_db()
    cursor = conn.cursor()
    
    cursor.execute("PRAGMA table_info(stacks)")
    columns = {row[1]: row[2] for row in cursor.fetchall()}
    
    assert columns["stack_id"] == "TEXT"
    assert columns["name"] == "TEXT"
    assert columns["current_version"] == "TEXT"
    assert columns["created_at"] == "TEXT"
    assert columns["updated_at"] == "TEXT"
    
    conn.close()


def test_stack_versions_table_schema():
    """Test the schema of the stack_versions table."""
    conn = init_db()
    cursor = conn.cursor()
    
    cursor.execute("PRAGMA table_info(stack_versions)")
    columns = {row[1]: row[2] for row in cursor.fetchall()}
    
    assert columns["stack_id"] == "TEXT"
    assert columns["version"] == "TEXT"
    assert columns["parent_version"] == "TEXT"
    assert columns["created_at"] == "TEXT"
    assert columns["created_by"] == "TEXT"
    assert columns["comment"] == "TEXT"
    assert columns["compose_definition"] == "TEXT"
    assert columns["runtime_mapping"] == "TEXT"
    assert columns["status"] == "TEXT"
    
    conn.close()


def test_commands_table_schema():
    """Test the schema of the commands table."""
    conn = init_db()
    cursor = conn.cursor()
    
    cursor.execute("PRAGMA table_info(commands)")
    columns = {row[1]: row[2] for row in cursor.fetchall()}
    
    assert columns["command_id"] == "INTEGER"
    assert columns["stack_id"] == "TEXT"
    assert columns["type"] == "TEXT"
    assert columns["payload"] == "TEXT"
    assert columns["status"] == "TEXT"
    assert columns["created_at"] == "TEXT"
    assert columns["started_at"] == "TEXT"
    assert columns["ended_at"] == "TEXT"
    
    conn.close()


def test_command_logs_table_schema():
    """Test the schema of the command_logs table."""
    conn = init_db()
    cursor = conn.cursor()
    
    cursor.execute("PRAGMA table_info(command_logs)")
    columns = {row[1]: row[2] for row in cursor.fetchall()}
    
    assert columns["log_id"] == "INTEGER"
    assert columns["command_id"] == "INTEGER"
    assert columns["timestamp"] == "TEXT"
    assert columns["level"] == "TEXT"
    assert columns["message"] == "TEXT"
    
    conn.close()
