from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from starlette.responses import FileResponse

from model_config import MODEL_CONFIGS
from utils.model_loader import ModelLoader
from utils.image_processing import preprocess_image, save_processed_image, process_image_with_model
import os
import time
from dotenv import load_dotenv
import uvicorn
from pathlib import Path
from fastapi.staticfiles import StaticFiles
# 加载环境变量
load_dotenv()

app = FastAPI(
    title="图像处理模型API服务",
    description="提供六个图像处理模型的API接口",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("ALLOWED_ORIGINS", "*").split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 初始化模型加载器
model_loader = ModelLoader(model_dir="models")

# 模型映射
MODEL_MAP = {
    "denoise": "denoise.onnx",
    "color_enhance": "color_enhance.onnx",  # 确保与Java端的modelId匹配
    "super_res_2x": "super_res_2x.onnx",
    "super_res_4x": "super_res_4x.onnx",
    "super_res_manga_4x": "super_res_manga_4x.onnx",
    "image_sharpening": "image_sharpening.onnx"
}

#配置环境变量或使用默认值
UPLOAD_DIR = os.getenv("UPLOAD_DIR", "uploads")
DOWNLOAD_DIR = os.getenv("DOWNLOAD_DIR", "download")
MODEL_DIR = os.getenv("MODEL_DIR", "models")

# 设置模型加载器的模型目录
model_loader.model_dir = Path(MODEL_DIR)

# 预加载所有模型
@app.on_event("startup")
async def load_models():
    print("开始加载模型...")

    # 确保模型目录存在
    model_dir = Path(MODEL_DIR)
    model_dir.mkdir(exist_ok=True)

    # 加载所有模型
    loaded_count = 0
    for model_name, model_file in MODEL_MAP.items():
        try:
            model_loader.load_model(model_file)
            loaded_count += 1
            print(f"模型 {model_file} 加载成功")
        except Exception as e:
            print(f"模型 {model_file} 加载失败: {str(e)}")

    print(f"模型加载完成! 成功加载 {loaded_count}/{len(MODEL_MAP)} 个模型")

    # 打印已加载的模型列表
    loaded_models = model_loader.get_loaded_models()
    print("已加载模型:", loaded_models)


# 健康检查
@app.get("/health")
def health_check():
    loaded_models= list(model_loader.models.keys())
    model_status= {}
    for model_file in loaded_models:
        session= model_loader.models[model_file]
        model_status
        [model_file] = {
            "providers": session.get_providers(),
            "input_info": [{"name": inp.name, "shape": inp.shape} for inp in session.get_inputs()],
            "output_info": [{"name": out.name, "shape": out.shape} for out in session.get_outputs()]
        }

    return {
    "status": "healthy",
    "models_loaded": loaded_models,
    "model_details":
        model_status
    }


# 通用模型处理函数

@app.post("/process")
async def process_image(
        file: UploadFile = File(...),
        model_name: str = Form("color_enhance")
):
    try:
        # 确保上传目录存在
        upload_dir = Path(UPLOAD_DIR)
        upload_dir.mkdir(exist_ok=True)

        # 确保处理目录存在
        processed_dir = Path(DOWNLOAD_DIR)
        processed_dir.mkdir(exist_ok=True)

        # 读取上传的文件
        contents = await file.read()

        # 保存上传的文件
        upload_filename = f"upload_{int(time.time())}_{file.filename}"
        upload_path = upload_dir / upload_filename
        with open(upload_path, "wb") as f:
            f.write(contents)

        # 检查模型是否存在
        if model_name not in MODEL_CONFIGS:
            return {"error": f"未知模型: {model_name}"}

        config = MODEL_CONFIGS[model_name]
        model_file = config.model_file

        # 检查模型文件是否存在
        model_path = Path(MODEL_DIR) / model_file
        if not model_path.exists():
            return {"error": f"模型文件不存在: {model_path}"}

        # 处理图像 - 这里传递模型名称而不是模型路径
        output, original_size, padding = process_image_with_model(config, contents)

        # 生成输出文件名
        output_filename = f"processed_{file.filename}"
        output_path = processed_dir / output_filename

        # 保存处理后的图像
        clean_filename = save_processed_image(
            output, str(output_path), original_size, padding, config, contents
        )

        # 返回处理结果信息（与Java后端期望的格式匹配）
        return {
            "status": "success",
            "model": model_name,
            "processed_path": str(processed_dir / clean_filename),
            "result_url": f"/processed/{clean_filename}"  # 使用Java后端期望的字段名
        }

    except Exception as e:
        print(f"Error processing image: {e}")
        import traceback
        traceback.print_exc()
        return {"status": "error", "error": str(e)}

@app.get("/processed/{filename}")
async def download_file(filename: str):
    file_path = Path(DOWNLOAD_DIR) / filename

    if not file_path.exists():
        raise HTTPException(status_code=404, detail="文件不存在")

    # 返回文件响应
    return FileResponse(
        file_path,
        media_type="image/png",
        filename=f"processed_{filename}"
    )

# 模型信息端点
@app.get("/models")
async def get_models_info():
    """获取所有模型的信息"""
    models_info = {}
    for model_name, config in MODEL_CONFIGS.items():
        models_info
        [model_name] = {
            "model_file": config.model_file,
            "preprocess": config.preprocess.dict(),
            "postprocess": config.postprocess.dict(),
            "special_processing": config.
        special_processing
        }
    return models_info

# 提供处理结果的静态文件访问
app.mount("/processed", StaticFiles(directory=DOWNLOAD_DIR), name="processed")
# 保留原有的特定端点，但确保它们也使用正确的参数名
@app.post("/api/denoise")
async def denoise_model(file: UploadFile = File(...)):
    return await process_image(file, "denoise")


@app.post("/api/color_enhance")
async def color_enhancement_model(file: UploadFile = File(...)):
    return await process_image(file, "color_enhance")


@app.post("/api/super_res_2x")
async def super_res_2x_model(file: UploadFile = File(...)):
    return await process_image(file, "super_res_2x")


@app.post("/api/super_res_4x")
async def super_res_4x_model(file: UploadFile = File(...)):
    return await process_image(file, "super_res_4x")


@app.post("/api/super_res_manga_4x")
async def manga_sharpen_model(file: UploadFile = File(...)):
    return await process_image(file, "super_res_manga_4x")


@app.post("/api/image_sharpening")
async def image_sharpening_model(file: UploadFile = File(...)):
    return await process_image(file, "image_sharpening")


if __name__ == "__main__":
    uvicorn.run(
        app,
        host=os.getenv("HOST", "0.0.0.0"),
        port=int(os.getenv("PORT", 8000))
    )