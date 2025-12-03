# image_processing.py
from venv import logger

import cv2
import numpy as np
from PIL import Image
import io


# 从ModelLoader获取模型会话

import os
import re
from typing import Tuple, Dict, Any
from schemas import ModelConfig



def preprocess_image(
        image_bytes: bytes,
        config: ModelConfig,
        tile_size: int = 256,
        pad_size: int = 32
) -> Tuple[np.ndarray, Tuple[int, int], Tuple[int, int, int, int]]:
    logger.info(f"开始预处理图像，模型: {config.name}")

    # 检查是否是超分辨率模型
    if config.special_processing and config.special_processing.get("is_super_resolution", False):
        return super_resolution_preprocess(image_bytes, config)


    # 检查是否是简单模型 (color_enhance)
    if hasattr(config.preprocess, 'simple_preprocess') and config.preprocess.simple_preprocess:
        return simple_preprocess(image_bytes, config)
    else:
        return complex_preprocess(image_bytes, config, tile_size, pad_size)


def simple_preprocess(
        image_bytes: bytes,
        config: ModelConfig
) -> Tuple[np.ndarray, Tuple[int, int], Tuple[int, int, int, int]]:
    """处理色彩增强等简单模型"""
    logger.info(f"使用简单预处理流程，模型: {config.name}")

    image = Image.open(io.BytesIO(image_bytes))
    logger.info(f"原始图像模式: {image.mode}, 尺寸: {image.size}")

    # BGR
    img_array = np.array(image)
    if image.mode == 'RGBA':
        img_array = cv2.cvtColor(img_array, cv2.COLOR_RGBA2BGR)
    elif image.mode == 'RGB':
        img_array = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)

    # 保存原始尺寸
    original_h, original_w = img_array.shape[:2]

    # 处理图像大小
    if hasattr(config.preprocess, 'input_size') and config.preprocess.input_size:
        target_size = config.preprocess.input_size
        img_array = cv2.resize(img_array, target_size, interpolation=cv2.INTER_AREA)

    img_array = apply_normalization(img_array, config.preprocess)

    if len(img_array.shape) == 3:
        img_array = np.transpose(img_array, (2, 0, 1))

    img_array = np.expand_dims(img_array, axis=0)

    return img_array, (original_h, original_w), (0, 0, 0, 0)


def complex_preprocess(
        image_bytes: bytes,
        config: ModelConfig,
        tile_size: int = 256,
        pad_size: int = 32
) -> Tuple[np.ndarray, Tuple[int, int], Tuple[int, int, int, int]]:
    """图像去噪的复杂模型"""
    logger.info(f"使用复杂预处理流程，模型: {config.name}")

    # 从字节数据读取图像
    image = Image.open(io.BytesIO(image_bytes))
    logger.info(f"原始图像模式: {image.mode}, 尺寸: {image.size}")

    # 转换色彩空间
    if image.mode == 'RGBA':
        image = image.convert('RGB')
        logger.info("转换 RGBA 到 RGB")

    # BGR处理
    img_array = np.array(image)
    if len(img_array.shape) == 3 and img_array.shape[2] == 3:
        #根据配置决定色彩空间
        if config.preprocess.input_space == "RGB":
            #已经是，不做处理
            pass
        elif config.preprocess.input_space == "BGR":
            img_array = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)
            logger.info("转换 RGB 到 BGR")
        elif config.preprocess.input_space == "LAB":
            img_array = cv2.cvtColor(img_array, cv2.COLOR_RGB2LAB)
            logger.info("转换 RGB 到 LAB")

    # 获取原始尺寸
    original_h, original_w = img_array.shape[:2]
    logger.info(f"原始尺寸: {original_h}x{original_w}")

    # 计算调整后的尺寸
    new_h = ((original_h + tile_size - 1) // tile_size) * tile_size
    new_w = ((original_w + tile_size - 1) // tile_size) * tile_size
    logger.info(f"调整后尺寸: {new_h}x{new_w}")

    # 如果需要padding
    if new_h != original_h or new_w != original_w:
        # 创建新图像并粘贴原始图像信息
        if len(img_array.shape) == 3:
            new_image = np.zeros((new_h, new_w, img_array.shape[2]), dtype=img_array.dtype)
        else:
            new_image = np.zeros((new_h, new_w), dtype=img_array.dtype)

        paste_x = (new_w - original_w) // 2
        paste_y = (new_h - original_h) // 2
        new_image[paste_y:paste_y + original_h, paste_x:paste_x + original_w] = img_array

        # 计算padding信息
        top = paste_y
        bottom = new_h - original_h - top
        left = paste_x
        right = new_w - original_w - left

        logger.info(f"填充信息: top={top}, bottom={bottom}, left={left}, right={right}")
    else:
        new_image = img_array
        top = bottom = left = right = 0
        logger.info("无需填充")

    # 计算灰度图
    if config.special_processing and config.special_processing.get("requires_gray", False):
        gray_image = calculate_gray_attention(new_image, config)  # Pass numpy array
        logger.info(f"灰度图形状: {gray_image.shape}, 值范围: [{gray_image.min()}, {gray_image.max()}]")

        # 将灰度图像与原始图像拼接为第二通道
        if len(new_image.shape) == 3:
            # 如果已经是3通道图像，添加灰度作为第4通道
            new_image = np.concatenate([new_image, gray_image], axis=2)
        else:
            # 如果是单通道图像，添加灰度作为第二通道
            new_image = np.stack([new_image, gray_image], axis=2)

        logger.info(f"拼接后图像形状: {new_image.shape}")

    # 应用归一化
    new_image = apply_normalization(new_image, config.preprocess)
    logger.info(f"归一化后数据类型: {new_image.dtype}, 值范围: [{new_image.min()}, {new_image.max()}]")

    # 转换为CHW格式
    if len(new_image.shape) == 3:  # HWC format
        new_image = np.transpose(new_image, (2, 0, 1))  # HWC to CHW
        logger.info(f"转换 HWC 到 CHW, 新形状: {new_image.shape}")

    # 转换为CHW格式
    new_image = np.expand_dims(new_image, axis=0)  # (1, C, H, W)
    logger.info(f"添加批次维度后形状: {new_image.shape}")

    return new_image, (original_h, original_w), (top, bottom, left, right)
def calculate_gray_attention(image: np.ndarray, config: ModelConfig) -> np.ndarray:
    """计算灰度注意力图"""
    # 首先确保输入是 numpy 数组
    if not isinstance(image, np.ndarray):
        # 如果是 PIL Image 对象，转换为 numpy 数组
        image = np.array(image)

    if config.special_processing.get("gray_calculation", "weighted") == "weighted":
        # 使用加权平均计算灰度（与原始代码一致）
        if len(image.shape) == 3 and image.shape[2] == 3:  # RGB图像
            r, g, b = image[:, :, 0], image[:, :, 1], image[:, :, 2]
            # 根据原始代码中的公式：gray = 1. - (0.299*r + 0.587*g + 0.114*b)/2.
            gray = 1.0 - (0.299 * r + 0.587 * g + 0.114 * b) / 2.0
        else:
            # 单通道图像直接使用
            gray = image.copy()
    else:
        # 简单平均
        if len(image.shape) == 3 and image.shape[2] == 3:
            gray = np.mean(image, axis=2)
        else:
            gray = image.copy()

    # 确保灰度图是2D的
    if len(gray.shape) > 2:
        gray = np.squeeze(gray)

    # 添加通道维度
    gray = np.expand_dims(gray, axis=2)

    return gray


def apply_normalization(image: np.ndarray, preprocess_config) -> np.ndarray:
    """应用归一化"""
    #logger.info(f"应用归一化: {preprocess_config.normalization}")

    if preprocess_config.normalization == "0-1":
        image = image.astype(np.float32) / 255.0
        #logger.info("归一化到 [0, 1] 范围")
    elif preprocess_config.normalization == "-1-1":
        image = image.astype(np.float32) / 255.0  # 先转到[0,1]
        image = image * 2.0 - 1.0  # 然后转到[-1,1]
        #logger.info("归一化到 [-1, 1] 范围")
    # 0-255 不需要归一化

    # 应用均值标准差归一化
    if preprocess_config.mean and preprocess_config.std:
        #logger.info(f"应用均值标准差归一化: mean={preprocess_config.mean}, std={preprocess_config.std}")
        if len(image.shape) == 3:
            for i in range(min(3, image.shape[2])):
                image[:, :, i] = (image[:, :, i] - preprocess_config.mean[i]) / preprocess_config.std[i]
        else:
            image = (image - preprocess_config.mean[0]) / preprocess_config.std[0]

    return image


def apply_model_specific_preprocess(image: np.ndarray, config: ModelConfig) -> np.ndarray:
    """应用模型特定的预处理"""
    # 颜色空间转换
    if hasattr(config.preprocess, 'input_space'):
        if config.preprocess.input_space == "LAB":
            image = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
        elif config.preprocess.input_space == "RGB":
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)


    # 特殊处理
    if config.special_processing and "color_space_conversion" in config.special_processing:
        if config.special_processing["color_space_conversion"] == "BGR2LAB":
            image = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)

    return image


def apply_normalization(image: np.ndarray, preprocess_config) -> np.ndarray:
    """应用归一化"""
    if preprocess_config.normalization == "0-1":
        image = image.astype(np.float32) / 255.0
    elif preprocess_config.normalization == "-1-1":
        image = (image.astype(np.float32) / 127.5) - 1.0
    # 0-255 不需要归一化

    # 应用均值标准差归一化
    if preprocess_config.mean and preprocess_config.std:
        if len(image.shape) == 3:
            for i in range(3):
                image[..., i] = (image[..., i] - preprocess_config.mean[i]) / preprocess_config.std[i]
        else:
            image = (image - preprocess_config.mean[0]) / preprocess_config.std[0]

    return image



def process_image_with_model(config: ModelConfig, image_bytes: bytes) -> Tuple[
    np.ndarray, Tuple[int, int], Tuple[int, int, int, int]]:
    """
    使用ONNX模型处理图像

    参数:
        model_name: 模型名称
        image_bytes: 图像字节数据

    返回:
        tuple: (处理后的图像数组, 原始尺寸, 填充信息)
    """

    # # 检查是否是图像清晰化模型，如果是则直接返回原始图像
    # if config.name == "image_sharpening":
    #     # 预处理图像但不进行模型处理
    #     input_data, original_size, padding = preprocess_image(image_bytes, config)
    #     # 直接返回预处理后的数据作为"输出"
    #     return input_data, original_size,padding

    # 从ModelLoader获取模型会话
    from utils.model_loader import model_loader

    session = model_loader.get_model(config.model_file)

    # 预处理图像
    input_data, original_size, padding = preprocess_image(image_bytes,
                                                          config)

    # 运行模型
    input_names= [inp.name for inp in session.get_inputs()]

    # 对于超分辨率模型，使用简单的输入处理
    if config.special_processing and config.special_processing.get("is_super_resolution", False):
        input_name= session.get_inputs()[0].name
        output= session.run(None, {input_name: input_data})[0]
    else:
        # 对于color_enhance模型，可能需要多个输入
        if config.special_processing and config.special_processing.get("requires_gray", False):
            # 分离图像和灰度图
            if input_data.shape[1] == 4:  # 如果有4个通道
                image_input= input_data[:, :3, :, :]  # 前3通道是图像
                gray_input= input_data[:, 3:, :, :]  # 第4通道是灰度图
            else:
                # 如果没有4个通道，可能需要重新计算灰度图
                image_input=input_data
                gray_input= calculate_gray_attention(input_data, config)
                gray_input= np.expand_dims(gray_input, axis=0)  # 添加批次维度

            if len(input_names) == 2:
                # 有两个输入：图像和灰度图
                output= session.run(None, {
                    input_names[0]: image_input,
                    input_names[1]:gray_input
                })[0]
            else:
                # 只有一个输入，直接使用图像数据
                output= session.run(None, {input_names[0]: image_input})[0]
        else:
            # 只有一个输入
            input_name= session.get_inputs()[0].name
            output= session.run(None, {input_name: input_data})[0]

    return output, original_size, padding


def apply_model_specific_postprocess(image: np.ndarray, config: ModelConfig) -> np.ndarray:
    """应用模型特定的后处理"""
    # 颜色空间转换
    if hasattr(config.postprocess, 'output_space'):
        if config.postprocess.output_space == "LAB":
            image = cv2.cvtColor(image, cv2.COLOR_LAB2BGR)
        elif config.postprocess.output_space == "RGB":
            image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        # 其他颜色空间转换...

    # 特殊处理
    if config.special_processing and "color_space_conversion" in config.special_processing:
        if config.special_processing["color_space_conversion"] == "BGR2LAB":
            image = cv2.cvtColor(image, cv2.COLOR_LAB2BGR)

    return image


def apply_denormalization(image: np.ndarray, postprocess_config) -> np.ndarray:
    """应用反归一化"""
    if postprocess_config.denormalization == "0-1":
        image = image * 255.0
    elif postprocess_config.denormalization == "-1-1":
        image = (image + 1.0) * 127.5
    # 0-255 不需要反归一化

    return image

#-------------------------------对超分辨率组合的特定处理------------
def super_resolution_preprocess(
        image_bytes: bytes,
        config: ModelConfig
) -> Tuple[np.ndarray, Tuple[int, int], Tuple[int, int, int, int]]:
    """
    超分辨率模型的预处理
    """
    logger.info(f"超分辨率预处理，模型: {config.name}")

    # 从字节流读取图像
    nparr = np.frombuffer(image_bytes, np.uint8)
    image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if image is None:
        raise ValueError("无法解码图像")

    # 存储原始尺寸
    original_h, original_w = image.shape[:2]

    # 获取缩放因子
    scale_factor = config.special_processing.get("scale_factor", 2)

    # 裁剪到合适的尺寸（确保是缩放因子的倍数）
    new_h = original_h - (original_h % scale_factor)
    new_w = original_w - (original_w % scale_factor)

    # 裁剪图像
    if new_h != original_h or new_w != original_w:
        logger.info(f"裁剪图像从 {original_h}x{original_w} 到 {new_h}x{new_w}")
        image = image[:new_h, :new_w]
        original_h, original_w = new_h, new_w

    # 转换颜色空间 BGR -> RGB
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    # 归一化到 [0, 1]
    image_normalized = image_rgb.astype(np.float32) / 255.0

    # 转换为CHW格式
    image_chw = np.transpose(image_normalized, (2, 0, 1))

    # 添加批次维度
    image_batch = np.expand_dims(image_chw, axis=0)

    return image_batch, (original_h, original_w), (0, 0, 0, 0)

def super_resolution_postprocess(
        output: np.ndarray,
        original_size: Tuple[int, int],
        padding: Tuple[int, int, int, int],
        config: ModelConfig
) -> np.ndarray:
    """
    超分辨率模型的后处理
    """
    # 移除批次维度
    if len(output.shape) == 4:
        output = output[0]

    # 转换回HWC格式
    output = np.transpose(output, (1, 2, 0))

    # 裁剪到 [0, 1] 范围
    output = np.clip(output, 0, 1)

    # 转换到 [0, 255] 范围
    output = (output * 255).astype(np.uint8)

    # 转换颜色空间 RGB -> BGR
    output = cv2.cvtColor(output, cv2.COLOR_RGB2BGR)

    return output



def save_processed_image(
        output: np.ndarray,
        output_path: str,
        original_size: Tuple[int, int],
        padding: Tuple[int, int, int, int],
        config: ModelConfig,
        image_bytes: bytes = None  # 添加这个参数
) -> str:
    """
    保存处理后的图像，对于简单预处理模型直接调整尺寸，对于复杂预处理模型移除填充
    """

    # # 检查是否是图像清晰化模型
    # if config.name == "image_sharpening":
    #     # 对于图像清晰化模型，直接保存原始图像
    #     with open(output_path, "wb") as f:
    #         f.write(image_bytes)  # 这里需要访问原始图像字节
    #     return remove_timestamp_prefix(os.path.basename(output_path))


    # 检查是否是超分辨率模型
    if config.special_processing and config.special_processing.get("is_super_resolution", False):
        img_array= super_resolution_postprocess(output, original_size, padding, config)
    else:


        # 对于复杂预处理模型移除填充
        if len(output.shape) == 4:
            img_array = output[0]
        else:
            img_array = output

        # 转化为HWC
        if img_array.shape[0] == 3:  # CHW format
            img_array = np.transpose(img_array, (1, 2, 0))  # CHW to HWC

        # 反归一化
        img_array = apply_denormalization(img_array, config.postprocess)

        # 后处理调用
        img_array = apply_model_specific_postprocess(img_array, config)

        # 处理复杂模型
        if hasattr(config.preprocess, 'simple_preprocess') and config.preprocess.simple_preprocess:
            # 对于简单预处理，恢复原始尺寸
            original_h, original_w = original_size
            img_array = cv2.resize(img_array, (original_w, original_h), interpolation=cv2.INTER_LINEAR)
        else:
            #复杂预处理移除padding
            top, bottom, left, right = padding
            h, w = original_size
            img_array = img_array[top:top + h, left:left + w, :]

    # 转化为uint8
    img_array = np.clip(img_array, *config.postprocess.clip_range).astype('uint8')

    filename = os.path.basename(output_path)
    clean_filename = remove_timestamp_prefix(filename)

    dir_path = os.path.dirname(output_path)
    clean_output_path = os.path.join(dir_path, clean_filename)

    # 保存图片
    cv2.imwrite(clean_output_path, img_array)


    return clean_filename


def remove_timestamp_prefix(filename: str) -> str:
    """
    移除文件名中的时间戳前缀

    参数:
        filename: 原始文件名

    返回:
        str: 移除时间戳后的文件名
    """
    # 定义时间戳模式（10位数字后跟下划线）
    timestamp_pattern = r'^\d{10}_'

    # 移除时间戳前缀
    clean_filename = re.sub(timestamp_pattern, '', filename)

    return clean_filename