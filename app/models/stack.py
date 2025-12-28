from pydantic import BaseModel, Field
from typing import Optional


class Stack(BaseModel):
    stack_id: str = Field(min_length=1)
    name: str = Field(min_length=1)
    current_version: Optional[str] = None
