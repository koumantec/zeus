"""Configuration for Core Monitor application."""
import os
from pathlib import Path

# Base directory
BASE_DIR = Path(__file__).parent.parent

# Storage directory
STORAGE_DIR = BASE_DIR / "storage"
STORAGE_DIR.mkdir(exist_ok=True)

# Database configuration
DB_PATH = os.getenv("CORE_MONITOR_DB_PATH", str(STORAGE_DIR / "core-monitor.db"))

# Application configuration
APP_NAME = "Core Monitor"
APP_VERSION = "0.1.0"
