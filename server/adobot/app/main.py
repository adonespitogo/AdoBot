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
STARTED_MONOTONIC = time.monotonic()

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


async def diagnostics(request: Request) -> JSONResponse:
    del request

    uptime_seconds = max(
        0,
        int(time.monotonic() - STARTED_MONOTONIC),
    )

    return JSONResponse(
        {
            "service": APP_NAME,
            "status": "ok",
            "environment": ENVIRONMENT,
            "version": VERSION,
            "python": os.sys.version.split()[0],
            "pid": os.getpid(),
            "uptime_seconds": uptime_seconds,
            "endpoints": [
                "/",
                "/api",
                "/health",
                "/ready",
                "/info",
                "/diagnostics",
            ],
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



async def api_root(request: Request) -> JSONResponse:
    return JSONResponse(
        {
            "service": APP_NAME,
            "status": "ok",
            "environment": ENVIRONMENT,
            "version": VERSION,
            "api": {
                "health": "/health",
                "ready": "/ready",
                "info": "/info",
            },
        }
    )


routes = [
    Route("/api", api_root, methods=["GET"]),
    Route("/", index, methods=["GET"]),
    Route("/health", health, methods=["GET"]),
    Route("/ready", ready, methods=["GET"]),
    Route("/info", info, methods=["GET"]),
    Route("/diagnostics", diagnostics, methods=["GET"]),
]

app = Starlette(
    debug=False,
    routes=routes,
)

app.add_middleware(RequestLoggingMiddleware)
