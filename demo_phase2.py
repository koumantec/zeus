#!/usr/bin/env python3
"""
Quick demonstration of Phase 2 implementation.
Run this script to verify the persistence layer works correctly.
"""
from datetime import datetime

from app.db.database import Database
from app.models.command import Command
from app.models.stack import Stack
from app.models.stack_version import ComposeDefinition, StackMetadata, StackVersion


def main():
    print("=" * 60)
    print("Core Monitor - Phase 2 Demo")
    print("Persistance & Queue (SQLite)")
    print("=" * 60)
    print()

    # Use in-memory database for demo
    print("1. Initializing database (in-memory)...")
    with Database() as db:
        print("   ✓ Database connected\n")

        # Create a stack
        print("2. Creating a stack...")
        stack = Stack(
            stack_id="demo-001",
            name="demo-stack",
            current_version=None
        )
        db.stacks.create(stack)
        print(f"   ✓ Stack created: {stack.name}\n")

        # Create a version
        print("3. Creating stack version v1...")
        version = StackVersion(
            stack_id="demo-001",
            version="v1",
            parent_version=None,
            metadata=StackMetadata(
                created_at=datetime.utcnow(),
                created_by="demo-user",
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
        db.stack_versions.create(version)
        print(f"   ✓ Version created: {version.version}\n")

        # Enqueue a command
        print("4. Enqueuing APPLY command...")
        cmd = Command(
            stack_id="demo-001",
            type="APPLY_STACK_VERSION",
            payload={"version": "v1"},
            status="PENDING",
            created_at=datetime.utcnow()
        )
        db.commands.enqueue(cmd)
        print(f"   ✓ Command enqueued (ID: {cmd.command_id})\n")

        # Process the command
        print("5. Processing command queue...")
        next_cmd = db.commands.get_next_pending()
        if next_cmd:
            print(f"   → Processing command {next_cmd.command_id}")
            
            # Mark as running
            db.commands.update_status(
                next_cmd.command_id,
                "RUNNING",
                started_at=datetime.utcnow()
            )
            db.command_logs.add_log(
                next_cmd.command_id,
                "INFO",
                "Starting command execution"
            )
            
            # Simulate work
            db.command_logs.add_log(
                next_cmd.command_id,
                "INFO",
                "Creating Docker resources..."
            )
            
            # Mark as done
            db.commands.update_status(
                next_cmd.command_id,
                "DONE",
                ended_at=datetime.utcnow()
            )
            db.command_logs.add_log(
                next_cmd.command_id,
                "INFO",
                "Command completed successfully"
            )
            
            print("   ✓ Command processed\n")

        # Display logs
        print("6. Command logs:")
        logs = db.command_logs.get_logs(cmd.command_id)
        for log in logs:
            print(f"   [{log['level']}] {log['message']}")
        print()

        # Display summary
        print("7. Summary:")
        stacks = db.stacks.list_all()
        print(f"   Stacks: {len(stacks)}")
        
        versions = db.stack_versions.list_by_stack("demo-001")
        print(f"   Versions: {len(versions)}")
        
        commands = db.commands.list_by_stack("demo-001")
        print(f"   Commands: {len(commands)}")
        print(f"   Command status: {commands[0].status}")
        print()

    print("=" * 60)
    print("✅ Phase 2 implementation verified successfully!")
    print("=" * 60)


if __name__ == "__main__":
    main()
