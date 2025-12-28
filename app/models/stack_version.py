from pydantic import BaseModel, Field, model_validator
from typing import Any, Dict, Optional
from datetime import datetime


class StackMetadata(BaseModel):
    created_at: datetime
    created_by: str = Field(min_length=1)
    comment: Optional[str] = None


class ComposeDefinition(BaseModel):
    version: str = Field(min_length=1)
    services: Dict[str, Dict[str, Any]] = Field(min_length=1)

    # compose permet l'absence explicite de networks/volumes
    networks: Dict[str, Any] = {}
    volumes: Dict[str, Any] = {}

    @model_validator(mode="after")
    def validate_services_keys(self):
        # Empêche des noms de service vides
        for svc_name in self.services.keys():
            if not isinstance(svc_name, str) or not svc_name.strip():
                raise ValueError("compose.services contient un nom de service invalide")
        return self


class RuntimeMapping(BaseModel):
    networks: Dict[str, str] = {}
    volumes: Dict[str, str] = {}
    containers: Dict[str, Dict[str, str]] = {}


class StackStatus(BaseModel):
    desired: str = Field(min_length=1)
    actual: str = Field(min_length=1)
    last_updated: Optional[datetime] = None


class StackVersion(BaseModel):
    stack_id: str = Field(min_length=1)
    version: str = Field(min_length=1)
    parent_version: Optional[str] = None

    metadata: StackMetadata
    compose: ComposeDefinition

    runtime: Optional[RuntimeMapping] = None
    status: Optional[StackStatus] = None
