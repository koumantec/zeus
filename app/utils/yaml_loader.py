from __future__ import annotations

from pathlib import Path
import yaml

from app.models.stack_version import StackVersion


def load_stack_version_from_yaml(path: str | Path) -> StackVersion:
    p = Path(path)
    raw = p.read_text(encoding="utf-8")
    data = yaml.safe_load(raw)

    if not isinstance(data, dict):
        raise ValueError("Le YAML de StackVersion doit être un objet (mapping)")

    return StackVersion(**data)
