from typing import Optional, Tuple, Dict, Any

from pydantic import BaseModel


class ModelPreprocessConfig(BaseModel):
    input_space: str = "BGR"  # BGR, RGB, LAB, etc.
    normalization: str = "0-1"  # 0-1, -1-1, 0-255
    resize_method: str = "bilinear"
    mean: Optional[list] = None
    std: Optional[list] = None
    tile_size: int = 256
    pad_size: int = 32
    simple_preprocess: bool = False
    input_size: Optional[Tuple[int, int]] = None

class ModelPostprocessConfig(BaseModel):
    output_space: str = "BGR"  # BGR, RGB, LAB, etc.
    denormalization: str = "0-1"  # 0-1, -1-1, 0-255
    clip_range: tuple = (0, 255)

class ModelConfig(BaseModel):
    name: str
    model_file: str
    preprocess: ModelPreprocessConfig
    postprocess: ModelPostprocessConfig
    special_processing: Optional[Dict[str, Any]] = None