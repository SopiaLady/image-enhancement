package com.ganwork.service;

import com.ganwork.exception.ModelType;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class ModelTypeFactoryBean extends AbstractFactoryBean<ModelType> {

    private ModelType modelType;

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    @Override
    public Class<?> getObjectType() {
        return ModelType.class;
    }

    @Override
    protected ModelType createInstance() {
        return modelType;
    }
}