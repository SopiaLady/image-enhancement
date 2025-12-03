# model_config.py
from schemas import ModelConfig, ModelPreprocessConfig, ModelPostprocessConfig

# 模型配置
MODEL_CONFIGS = {
    "denoise": ModelConfig(
        name="denoise",
        model_file="denoise.onnx",
        preprocess=ModelPreprocessConfig(
            input_space="BGR",
            normalization="0-1",
            resize_method="bilinear",
            simple_preprocess=False,
        ),
        postprocess=ModelPostprocessConfig(
            output_space="BGR",
            denormalization="0-1",
            clip_range=(0, 255)
        )
    ),
    # 更新color_enhance模型的配置
    "color_enhance": ModelConfig(
        name="color_enhance",
        model_file="color_enhance.onnx",
        preprocess=ModelPreprocessConfig(
            input_space="RGB",  # 原始代码中使用RGB
            normalization="0-1",  # 归一化到[0, 1]
            resize_method="bilinear",
            mean=None,  # 用于反归一化
            simple_preprocess=True,
            input_size=(256, 256),
            std=None    # 用于反归一化
        ),
        postprocess=ModelPostprocessConfig(
            output_space="RGB",
            denormalization="-1-1",  # 从[-1, 1]反归一化
            clip_range=(0, 255)
        ),
        special_processing={
            "requires_gray": False,  # 需要灰度注意力图
            "gray_calculation": "weighted"  # 使用加权平均计算灰度
        }
),
    "super_res_2x": ModelConfig(
        name="super_res_2x",
        model_file="super_res_2x.onnx",
        preprocess=ModelPreprocessConfig(
            input_space="BGR",
            normalization="0-1",
            resize_method="bilinear",
            simple_preprocess=False,
            tile_size=0,
            pad_size=0
        ),
        postprocess=ModelPostprocessConfig(
            output_space="BGR",
            denormalization="0-1",
            clip_range=(0, 255)
        ),
        special_processing={
            "is_super_resolution":True,
            "scale_factor":2
        }
    ),
    "super_res_4x": ModelConfig(
        name="super_res_4x",
        model_file="super_res_4x.onnx",
        preprocess=ModelPreprocessConfig(
            input_space="BGR",
            normalization="0-1",
            resize_method="bilinear"
        ),
        postprocess=ModelPostprocessConfig(
            output_space="BGR",
            denormalization="0-1",
            clip_range=(0, 255)
        ),
        special_processing={
            "is_super_resolution": True,
            "scale_factor": 4
        }
    ),
    "super_res_manga_4x": ModelConfig(
        name="super_res_manga_4x",
        model_file="super_res_manga_4x.onnx",
        preprocess=ModelPreprocessConfig(
            input_space="BGR",
            normalization="0-1",
            resize_method="bilinear"
        ),
        postprocess=ModelPostprocessConfig(
            output_space="BGR",
            denormalization="0-1",
            clip_range=(0, 255)
        ),
        special_processing={
            "is_super_resolution": True,
            "scale_factor": 4,
            "is_manga": True
        }
    ),

}

# 模型映射（保持与Java后端兼容）
MODEL_MAP = {name: config.model_file for name, config in MODEL_CONFIGS.items()}