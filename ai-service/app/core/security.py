from hmac import compare_digest

from fastapi import Header, HTTPException, status

from app.core.config import get_settings


def verify_internal_token(x_internal_token: str = Header(alias="X-Internal-Token")) -> None:
    if not compare_digest(x_internal_token, get_settings().ai_internal_token):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="内部服务认证失败")
