package com.ganwork.service;

import com.ganwork.exception.ModelType;

public interface ImageProcessingStrategy {
    String process(String inputFilename, ModelType.InternalModel internalModel, String imageType, int scale);
}