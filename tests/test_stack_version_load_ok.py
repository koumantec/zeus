from app.utils.yaml_loader import load_stack_version_from_yaml


def test_load_stack_version_ok(tmp_path):
    yml = """
stack_id: my-stack
version: v1
parent_version: null
metadata:
  created_at: 2025-01-10T10:00:00Z
  created_by: alice
compose:
  version: "3.9"
  services:
    postgres:
      image: postgres:15
  volumes:
    pg-data: {}
"""
    p = tmp_path / "sv.yml"
    p.write_text(yml, encoding="utf-8")

    sv = load_stack_version_from_yaml(p)
    assert sv.stack_id == "my-stack"
    assert sv.version == "v1"
    assert "postgres" in sv.compose.services
    assert sv.compose.volumes == {"pg-data": {}}
    assert sv.metadata.created_by == "alice"
    assert sv.metadata.created_at.isoformat() == "2025-01-10T10:00:00+00:00"
    assert sv.parent_version is None
    assert sv.runtime is None
    assert sv.status is None
    assert sv.compose.networks == {}
    assert sv.compose.services["postgres"]["image"] == "postgres:15"