# config.py
from pydantic import BaseModel, BaseSettings
from typing import Dict, Optional
from pathlib import Path
import os


class ModelEndpointConfig(BaseModel):
    api_endpoint: str
    api_key: str


class ModelMapConfig(BaseModel):
    modelMap: Dict[str, ModelEndpointConfig]


class CloudConfig(BaseModel):
    models: ModelMapConfig


class AppConfig(BaseSettings):
    cloud: CloudConfig
    upload_dir: str = "./uploads"
    cors_allowed_origins: str = "http://localhost:8080"

    class Config:
        env_file = ".env"
        env_prefix = "APP_"
        env_nested_delimiter = "__"


def load_config() -> AppConfig:
    # 可以从环境变量或配置文件加载
    return AppConfig(
        cloud=CloudConfig(
            models=ModelMapConfig(
                modelMap={
                    "DENOISE": ModelEndpointConfig(
                        api_endpoint="http://localhost:5000/process/denoise",
                        api_key="denoise_key_123"
                    ),
                    "COLOR_ENHANCEMENT": ModelEndpointConfig(
                        api_endpoint="http://localhost:5000/process/color_enhancement",
                        api_key="222"
                    ),
                    "COMBO_RES_SHARPEN_2X": ModelEndpointConfig(
                        api_endpoint="http://localhost:5000/process/super-res_2x",
                        #api_key="sr_key_456"
                    ),
                    "COMBO_RES_SHARPEN_4X": ModelEndpointConfig(
                        api_endpoint="http://localhost:5000/process/super-res_4x",
                        #api_key="sr_key_456"
                    ),
                    "COMBO_RES_MANGA_SHARPEN": ModelEndpointConfig(
                        api_endpoint="http://localhost:5000/process/super_res_manga_4x",
                        #api_key="sr_key_456"
                    )
                }
            )
        )
    )


# 全局配置实例
config = load_config()