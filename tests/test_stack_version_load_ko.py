import pytest
from app.utils.yaml_loader import load_stack_version_from_yaml


def test_load_stack_version_ko_missing_services(tmp_path):
    yml = """
stack_id: my-stack
version: v1
metadata:
  created_at: 2025-01-10T10:00:00Z
  created_by: alice
compose:
  version: "3.9"
  services: {}
"""
    p = tmp_path / "bad.yml"
    p.write_text(yml, encoding="utf-8")

    with pytest.raises(Exception):
        load_stack_version_from_yaml(p)


def test_load_stack_version_ko_not_a_mapping(tmp_path):
    yml = """
- a
- b
"""
    p = tmp_path / "bad2.yml"
    p.write_text(yml, encoding="utf-8")

    with pytest.raises(ValueError):
        load_stack_version_from_yaml(p)
