#!/usr/bin/env python3

from __future__ import annotations

import logging
import os
import time
from pathlib import Path

from starlette.applications import Starlette
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse
from starlette.routing import Route

ROOT = Path(__file__).resolve().parents[1]

APP_NAME = os.environ.get("APP_NAME", "adobot-server")
ENVIRONMENT = os.environ.get("ENVIRONMENT", "isolated")
VERSION = "0.1.0"

LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()

logging.basicConfig(
    level=getattr(logging, LOG_LEVEL, logging.INFO),
    format=(
        "%(asctime)s %(levelname)s "
        "service=adobot-server pid=%(process)d %(message)s"
    ),
)

logger = logging.getLogger("adobot-server")


async def index(request: Request) -> JSONResponse:
    return JSONResponse(
        {
            "service": APP_NAME,
            "status": "ok",
            "environment": ENVIRONMENT,
            "version": VERSION,
        }
    )


async def health(request: Request) -> JSONResponse:
    return JSONResponse(
        {
            "service": APP_NAME,
            "status": "healthy",
            "environment": ENVIRONMENT,
        }
    )


async def ready(request: Request) -> JSONResponse:
    return JSONResponse(
        {
            "service": APP_NAME,
            "status": "ready",
            "environment": ENVIRONMENT,
        }
    )


async def info(request: Request) -> JSONResponse:
    return JSONResponse(
        {
            "service": APP_NAME,
            "version": VERSION,
            "environment": ENVIRONMENT,
            "python": os.sys.version.split()[0],
            "root": str(ROOT),
        }
    )


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        started = time.monotonic()

        try:
            response = await call_next(request)
        except Exception:
            elapsed = time.monotonic() - started
            logger.exception(
                "request method=%s path=%s status=500 duration_ms=%.2f",
                request.method,
                request.url.path,
                elapsed * 1000,
            )
            raise

        elapsed = time.monotonic() - started

        logger.info(
            "request method=%s path=%s status=%s duration_ms=%.2f",
            request.method,
            request.url.path,
            response.status_code,
            elapsed * 1000,
        )

        return response


routes = [
    Route("/", index, methods=["GET"]),
    Route("/health", health, methods=["GET"]),
    Route("/ready", ready, methods=["GET"]),
    Route("/info", info, methods=["GET"]),
]

app = Starlette(
    debug=False,
    routes=routes,
)

app.add_middleware(RequestLoggingMiddleware)
