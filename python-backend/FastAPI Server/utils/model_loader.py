# model_loader.py
import onnxruntime as ort
import os
from pathlib import Path
from typing import Dict


class ModelLoader:
    _instance = None

    def __new__(cls, model_dir="models"):
        if cls._instance is None:
            cls._instance = super(ModelLoader, cls).__new__(cls)
            cls._instance.models = {}
            cls._instance.model_dir = Path(model_dir)
            # 确保模型目录存在
            cls._instance.model_dir.mkdir(exist_ok=True)
        return cls._instance

    def __init__(self, model_dir="models"):
        # 这里不需要再次初始化，已经在 __new__ 中处理了
        pass

    def load_model(self, model_file: str) -> ort.InferenceSession:
        """加载ONNX模型"""
        print(f"DEBUG: 尝试加载模型: {model_file}, 调用栈:")
        import traceback
        traceback.print_stack()

        if model_file in self.models:
            return self.models[model_file]

        model_path = self.model_dir / model_file
        if not model_path.exists():
            raise FileNotFoundError(f"模型文件不存在: {model_path}")

        # 创建推理会话
        sess_options = ort.SessionOptions()

        # 配置执行提供者 (优先使用GPU)
        providers = ['CUDAExecutionProvider', 'CPUExecutionProvider']

        try:
            session = ort.InferenceSession(
                str(model_path),
                providers=providers,
                sess_options=sess_options
            )
            self.models[model_file] = session
            print(f"模型加载成功: {model_file}")
            return session
        except Exception as e:
            print(f"模型加载失败: {model_file}, 错误: {e}")
            # 尝试使用CPU提供者
            try:
                session = ort.InferenceSession(
                    str(model_path),
                    providers=['CPUExecutionProvider'],
                    sess_options=sess_options
                )
                self.models[model_file] = session
                print(f"模型加载成功 (CPU): {model_file}")
                return session
            except Exception as e2:
                raise RuntimeError(f"无法加载模型 {model_file}: {e2}")

    def get_model(self, model_file: str) -> ort.InferenceSession:
        """获取已加载的模型"""
        if model_file in self.models:
            return self.models[model_file]
        raise ValueError(f"模型未加载: {model_file}")

    def get_loaded_models(self) -> list:
        """获取已加载的模型列表"""
        return list(self.models.keys())

    def unload_model(self, model_file: str) -> bool:
        """卸载模型"""
        if model_file in self.models:
            del self.models[model_file]
            print(f"模型已卸载: {model_file}")
            return True
        return False

    def load_all_models(self, model_files: list = None):
        """加载所有指定的模型文件，如果不指定则加载目录下所有 .onnx 文件"""
        if model_files is None:
            # 加载目录下所有 .onnx 文件
            model_files = [f.name for f in self.model_dir.glob("*.onnx")]

        loaded_count = 0
        for model_file in model_files:
            try:
                self.load_model(model_file)
                loaded_count += 1
            except Exception as e:
                print(f"加载模型 {model_file} 失败: {e}")

        return loaded_count


# 全局模型加载器实例
model_loader = ModelLoader()