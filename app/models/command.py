from pydantic import BaseModel, Field
from typing import Any, Dict, Optional
from datetime import datetime


class Command(BaseModel):
    command_id: Optional[int] = None
    stack_id: str = Field(min_length=1)
    type: str = Field(min_length=1)
    payload: Dict[str, Any] = {}

    status: str = Field(default="PENDING")
    created_at: datetime
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
