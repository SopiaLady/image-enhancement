package com.ganwork.exception;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ModelType {

    // 基础模型（独立功能）
    DENOISE("图像去噪", "denoise", "denoise"),
    COLOR_ENHANCEMENT("色彩增强", "color_enhance", "color_enhance"),
    IMAGE_SHARPENING("图像清晰化", "image_sharpening", "image_sharpening"),
    // 组合模型（包含超分辨率+清晰化）
    COMBO_RES_SHARPEN_2X("超分2倍+清晰化", "sr_sharpen_2x", "combo_res_sharpen_2x"),
    COMBO_RES_SHARPEN_4X("超分4倍+清晰化", "sr_sharpen_4x", "combo_res_sharpen_4x"),
    COMBO_RES_MANGA_SHARPEN("漫画超分+清晰化", "sr_manga_sharpen", "combo_res_manga_sharpen");

    private final String displayName;
    private final String commandPrefix;
    private final String requestParam;

    ModelType(String displayName, String commandPrefix, String requestParam) {
        this.displayName = displayName;
        this.commandPrefix = commandPrefix;
        this.requestParam = requestParam;
    }

    // 获取组合模型的内部处理流程
    public List<InternalModel> getProcessingPipeline() {
        switch (this) {
            case COMBO_RES_SHARPEN_2X:
                return Arrays.asList(
                        InternalModel.SUPER_RES_2X,
                        InternalModel.IMAGE_SHARPENING
                );
            case COMBO_RES_SHARPEN_4X:
                return Arrays.asList(
                        InternalModel.SUPER_RES_4X,
                        InternalModel.IMAGE_SHARPENING
                );
            case COMBO_RES_MANGA_SHARPEN:
                return Arrays.asList(
                        InternalModel.SUPER_RES_MANGA_4X,
                        InternalModel.IMAGE_SHARPENING
                );
            default:
                return Collections.singletonList(
                        InternalModel.fromModelType(this)
                );
        }
    }

    // 内部模型定义 - 改为公共
    public enum InternalModel {
        DENOISE("denoise"),   //降噪
        COLOR_ENHANCE("color_enhance"),   //色彩增强
        SUPER_RES_2X("super_res_2x"),   //普通图片2倍率
        SUPER_RES_4X("super_res_4x"),  //普通图片4倍率
        SUPER_RES_MANGA_4X("super_res_manga_4x"),   //漫画图片4倍率
        IMAGE_SHARPENING("image_sharpening");   //图像变清晰

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
                default: throw new IllegalArgumentException("无法转换的模型类型");
            }
        }
    }

    // 添加获取内部模型显示名称的方法
    public static String getInternalModelDisplayName(InternalModel model) {
        switch (model) {
            case SUPER_RES_2X: return "超分辨率(2倍)";
            case SUPER_RES_4X: return "超分辨率(4倍)";
            case SUPER_RES_MANGA_4X: return "漫画超分辨率(4倍)";
            case IMAGE_SHARPENING: return "图像清晰化";
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