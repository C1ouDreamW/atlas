import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


load_dotenv()


def _get_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "y", "on"}


def _get_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None or raw.strip() == "":
        return default
    return int(raw)


def _get_float(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None or raw.strip() == "":
        return default
    return float(raw)


@dataclass(frozen=True)
class Settings:
    redis_url: str = os.getenv("REDIS_URL", "redis://localhost:6379/0")
    redis_stream: str = os.getenv("REDIS_STREAM", "ishua:task:stream")
    redis_group: str = os.getenv("REDIS_GROUP", "ishua-ai-workers")
    redis_consumer: str = os.getenv("REDIS_CONSUMER", "worker_node_1")
    redis_block_ms: int = _get_int("REDIS_BLOCK_MS", 5000)

    processing_ttl_seconds: int = _get_int("PROCESSING_TTL_SECONDS", 600)
    result_ttl_seconds: int = _get_int("RESULT_TTL_SECONDS", 1800)
    heartbeat_interval_seconds: int = _get_int("HEARTBEAT_INTERVAL_SECONDS", 180)

    mineru_token: str = os.getenv("MINERU_TOKEN", "")
    mineru_base_url: str = os.getenv("MINERU_BASE_URL", "https://mineru.net/api/v4").rstrip("/")
    mineru_model_version: str = os.getenv("MINERU_MODEL_VERSION", "vlm")
    mineru_language: str = os.getenv("MINERU_LANGUAGE", "ch")
    mineru_enable_formula: bool = _get_bool("MINERU_ENABLE_FORMULA", True)
    mineru_enable_table: bool = _get_bool("MINERU_ENABLE_TABLE", True)
    mineru_is_ocr: bool = _get_bool("MINERU_IS_OCR", False)
    mineru_poll_interval_seconds: int = _get_int("MINERU_POLL_INTERVAL_SECONDS", 10)
    mineru_poll_timeout_seconds: int = _get_int("MINERU_POLL_TIMEOUT_SECONDS", 1800)
    mineru_http_timeout_seconds: int = _get_int("MINERU_HTTP_TIMEOUT_SECONDS", 60)

    llm_api_key: str = os.getenv("LLM_API_KEY", "")
    llm_base_url: str = os.getenv("LLM_BASE_URL", "https://api.deepseek.com")
    llm_model: str = os.getenv("LLM_MODEL", "deepseek-chat")
    llm_timeout_seconds: int = _get_int("LLM_TIMEOUT_SECONDS", 120)
    llm_temperature: float = _get_float("LLM_TEMPERATURE", 0.0)

    log_level: str = os.getenv("LOG_LEVEL", "INFO")

    debug_mode: bool = _get_bool("DEBUG_MODE", False)
    debug_temp_dir: str = os.getenv(
        "DEBUG_TEMP_DIR",
        str(Path(__file__).resolve().parent / "temp"),
    )
    skip_llm: bool = _get_bool("SKIP_LLM", False)

    def validate(self) -> None:
        if not self.mineru_token:
            raise ValueError("MINERU_TOKEN is required")
        if not self.skip_llm and not self.llm_api_key:
            raise ValueError("LLM_API_KEY is required (or set SKIP_LLM=true to skip LLM)")


settings = Settings()
