package com.ganwork.exception;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ModelType {

    // 基础模型（独立功能）
    DENOISE("图像去噪", "denoise", "denoise"),
    COLOR_ENHANCEMENT("色彩增强", "color_enhance", "color_enhance"),
    // 独立的超分辨率模型（不再是组合模型）
    SUPER_RES_2X("超分辨率2倍", "super_res_2x", "super_res_2x"),
    SUPER_RES_4X("超分辨率4倍", "super_res_4x", "super_res_4x"),
    SUPER_RES_MANGA_4X("漫画超分辨率", "super_res_manga_4x", "super_res_manga_4x");

    private final String displayName;
    private final String commandPrefix;
    private final String requestParam;

    ModelType(String displayName, String commandPrefix, String requestParam) {
        this.displayName = displayName;
        this.commandPrefix = commandPrefix;
        this.requestParam = requestParam;
    }

    // 获取模型的内部处理流程 - 简化为单一模型处理
    public List<InternalModel> getProcessingPipeline() {
        switch (this) {
            // 超分辨率模型现在是单一模型处理
            case SUPER_RES_2X:
                return Collections.singletonList(InternalModel.SUPER_RES_2X);
            case SUPER_RES_4X:
                return Collections.singletonList(InternalModel.SUPER_RES_4X);
            case SUPER_RES_MANGA_4X:
                return Collections.singletonList(InternalModel.SUPER_RES_MANGA_4X);
            default:
                return Collections.singletonList(
                        InternalModel.fromModelType(this)
                );
        }
    }

    // 内部模型定义 -
    public enum InternalModel {
        DENOISE("denoise"),   //降噪
        COLOR_ENHANCE("color_enhance"),   //色彩增强
        SUPER_RES_2X("super_res_2x"),   //普通图片2倍率
        SUPER_RES_4X("super_res_4x"),  //普通图片4倍率
        SUPER_RES_MANGA_4X("super_res_manga_4x");   //漫画图片4倍率

        private final String modelId;

        InternalModel(String modelId) {
            this.modelId = modelId;
        }

        public String getModelId() {
            return modelId;
        }

        // 修复 fromString 方法
        public static InternalModel fromString(String text) {
            for (InternalModel type : values()) {
                if (type.modelId.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的内部模型类型: " + text);
        }

        public static InternalModel fromModelType(ModelType type) {
            switch (type) {
                case DENOISE: return DENOISE;
                case COLOR_ENHANCEMENT: return COLOR_ENHANCE;
                case SUPER_RES_2X: return SUPER_RES_2X;
                case SUPER_RES_4X: return SUPER_RES_4X;
                case SUPER_RES_MANGA_4X: return SUPER_RES_MANGA_4X;
                default: throw new IllegalArgumentException("无法转换的模型类型");
            }
        }
    }

    // 添加获取内部模型显示名称的方法 - 移除 IMAGE_SHARPENING 相关
    public static String getInternalModelDisplayName(InternalModel model) {
        switch (model) {
            case SUPER_RES_2X: return "超分辨率(2倍)";
            case SUPER_RES_4X: return "超分辨率(4倍)";
            case SUPER_RES_MANGA_4X: return "漫画超分辨率(4倍)";
            case DENOISE: return "图像去噪";
            case COLOR_ENHANCE: return "色彩增强";
            default: return model.getModelId();
        }
    }

    public String getRequestParam() {
        return requestParam;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public static ModelType fromRequestParam(String param) {
        for (ModelType type : values()) {
            if (type.requestParam.equals(param)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的模型类型: " + param);
    }
}